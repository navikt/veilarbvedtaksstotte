package no.nav.veilarbvedtaksstotte.controller

import no.nav.common.audit_log.cef.AuthorizationDecision
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.common.audit_log.log.AuditLogger
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.AuthService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class AuditlogService(
    private val authService: AuthService,
    private val auditLogger: AuditLogger,
    private val vedtaksstotteRepository: VedtaksstotteRepository,
    private val aktorOppslagClient: AktorOppslagClient,
    @Value("\${NAIS_CLUSTER_NAME:local}") private val clusterName: String
) {
    private val log = LoggerFactory.getLogger(AuditlogService::class.java)

    fun auditlog(loggmelding: String, eksternBruker: Fnr?) {
        if (authService.erInternBruker() && eksternBruker != null) {
            val veilederIdent = authService.innloggetVeilederIdent
            auditLogger.log(
                CefMessage.builder()
                    .timeEnded(System.currentTimeMillis())
                    .applicationName("veilarbvedtaksstotte")
                    .sourceUserId(veilederIdent)
                    .authorizationDecision(AuthorizationDecision.PERMIT)
                    .event(CefMessageEvent.ACCESS)
                    .severity(CefMessageSeverity.INFO)
                    .name("veilarbvedtaksstotte-audit-log")
                    .destinationUserId(eksternBruker.get())
                    .extension("msg", loggmelding)
                    .build()
            )
            // Speil til OpenSearch i ikke-prod for testveileder Z994789
            if (veilederIdent == "Z994789" && clusterName != "prod-gcp") {
                log.info("Arcsight-logging: Testveileder {} {} [bruker:{}]", veilederIdent, loggmelding, maskertBruker(eksternBruker))
            }
        }
    }

    /** SHA-256 av FNR, trunkert til 8 hex-tegn. Deterministisk og ikke reversibel. */
    private fun maskertBruker(fnr: Fnr): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(fnr.get().toByteArray())
        return hash.take(4).joinToString("") { "%02x".format(it) }
    }

    fun finnFodselsnummerFraVedtakId(vedtakId: Long): Fnr? {
        val vedtak = vedtaksstotteRepository.hentVedtak(vedtakId) ?: null
        val aktorId = AktorId.of(vedtak?.aktorId)
        return try {
            aktorOppslagClient.hentFnr(aktorId)
        } catch (e: Exception) {
            null
        }
    }
}

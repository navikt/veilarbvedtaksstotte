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
import org.springframework.stereotype.Component

@Component
class AuditlogService(
    private val authService: AuthService,
    private val auditLogger: AuditLogger,
    private val vedtaksstotteRepository: VedtaksstotteRepository,
    private val aktorOppslagClient: AktorOppslagClient
) {

    fun auditlog(loggmelding: String, eksternBruker: Fnr?) {
        if (authService.erInternBruker() && eksternBruker != null) {
            auditLogger.log(
                CefMessage.builder()
                    .timeEnded(System.currentTimeMillis())
                    .applicationName("veilarbvedtaksstotte")
                    .sourceUserId(authService.innloggetVeilederIdent)
                    .authorizationDecision(AuthorizationDecision.PERMIT)
                    .event(CefMessageEvent.ACCESS)
                    .severity(CefMessageSeverity.INFO)
                    .name("veilarbvedtaksstotte-audit-log")
                    .destinationUserId(eksternBruker.get())
                    .extension("msg", loggmelding)
                    .build()
            )
        }
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

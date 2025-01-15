package no.nav.veilarbvedtaksstotte.service

import io.getunleash.DefaultUnleash
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.config.EnvironmentProperties
import no.nav.veilarbvedtaksstotte.domain.statistikk.SakStatistikk
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.SAK_STATISTIKK_PAA
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private const val AVSENDER = "Oppfølgingsvedtak § 14 a"

@Service
class SakStatistikkService @Autowired constructor(
    private val sakStatistikkRepository: SakStatistikkRepository,
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val bigQueryService: BigQueryService,
    private val unleashClient: DefaultUnleash,
    private val environmentProperties: EnvironmentProperties,
    private val vedtaksstotteRepository: VedtaksstotteRepository,
    private val siste14aVedtakService: Siste14aVedtakService

) {
    fun lagreSakstatistikkrad(vedtak: Vedtak, fnr: Fnr) {
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)
        val log: Logger = LoggerFactory.getLogger(SakStatistikkRepository::class.java)
        if (statistikkPaa) {
            val oppfolgingsperiode = veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr)
            val eksisterendeVedtak = vedtaksstotteRepository.hentFattedeVedtak(vedtak.aktorId)
            val siste14aVedtakArena = siste14aVedtakService.siste14aVedtakFraArena(fnr)
            var nestSisteBehandlingId: Long? = null
            var nestSisteNyeVedtak: Vedtak? = null
            var behandlingType = "VEDTAK"
            var relatertFagsystem: String? = null

            if (eksisterendeVedtak.size > 1) {
                val oppfolgingsperiodeStartDato = oppfolgingsperiode.get().startDato.toLocalDateTime()

                val filteredVedtak =
                    eksisterendeVedtak.filter { it.vedtakFattet?.isAfter(oppfolgingsperiodeStartDato) == true && it.id != vedtak.id }

                nestSisteNyeVedtak = filteredVedtak.maxByOrNull { it.id }
                nestSisteBehandlingId = nestSisteNyeVedtak?.id
                relatertFagsystem = AVSENDER

            }
            log.info("siste14aVedtakArena: {}", siste14aVedtakArena)
            if (siste14aVedtakArena != null) {
                if (siste14aVedtakArena.fraDato.isAfter(oppfolgingsperiode.get().startDato.toLocalDate())) {
                    if (nestSisteNyeVedtak != null) {
                        if (siste14aVedtakArena.fraDato.isAfter(nestSisteNyeVedtak.vedtakFattet.toLocalDate())) {
                            nestSisteBehandlingId = siste14aVedtakArena.vedtakId
                            relatertFagsystem = "Arena"
                        }
                    }
                }
            }
            if (nestSisteBehandlingId != null) {
                behandlingType = "REVURDERING"
            }
            val sakId = veilarboppfolgingClient.hentOppfolgingsperiodeSak(oppfolgingsperiode.get().uuid).sakId

            val nySakstatistikkrad = SakStatistikk(
                behandlingId = vedtak.id.toBigInteger(),
                aktorId = vedtak.aktorId,
                oppfolgingPeriodeUUID = oppfolgingsperiode.get().uuid,
                behandlingUuid = vedtak.referanse,
                relatertBehandlingId = nestSisteBehandlingId?.toBigInteger(), //dersom dette er nr 2 eller mer i oppfolgingsperioden
                relatertFagsystem = relatertFagsystem, //dersom dette er nr 2 eller mer i oppfolgingsperioden,
                sakId = sakId.toString(),
                mottattTid = oppfolgingsperiode.get().startDato.toLocalDateTime(),
                registrertTid = vedtak.utkastOpprettet,
                ferdigbehandletTid = LocalDateTime.now(),
                endretTid = LocalDateTime.now(),
                tekniskTid = LocalDateTime.now(),
                sakYtelse = "hm", //hva er dette?
                behandlingType = behandlingType, //dersom dette er nr 2 eller mer i oppfolgingsperioden blir det REVURDERING,
                behandlingStatus = "SENDT", //Når får vedtaket status sendt
                behandlingResultat = vedtak.innsatsgruppe.name,
                behandlingMetode = "MANUELL",
                innsatsgruppe = vedtak.innsatsgruppe.name,
                hovedmal = vedtak.hovedmal.name,
                opprettetAv = vedtak.veilederIdent,
                saksbehandler = vedtak.veilederIdent,
                ansvarligBeslutter = vedtak.veilederIdent, //dersom kvalitetsstikrer så blir dette en annen
                ansvarligEnhet = vedtak.oppfolgingsenhetId,
                avsender = AVSENDER,
                versjon = environmentProperties.naisAppImage
            )

            try {
                sakStatistikkRepository.insertSakStatistikkRad(nySakstatistikkrad)
                bigQueryService.logEvent(nySakstatistikkrad)
            } catch (e: Exception) {
                secureLog.error("Kunne ikke lagre sakstatistikk", e)
            }

        }
    }

    fun hentStatistikkRader(oppfolgingsperiodeUuid: UUID): Boolean {
        val statistikkListe =
            sakStatistikkRepository.hentSakStatistikkListeInnenforOppfolgingsperiode(oppfolgingsperiodeUuid)
        val antallUtkast =
            statistikkListe.stream().filter { item: SakStatistikk -> item.behandlingMetode == "UTKAST" }.toList().size
        return antallUtkast == 0
    }

    fun leggTilStatistikkRadUtkast(
        behandlingId: Long, aktorId: String, fnr: Fnr, veilederIdent: String, oppfolgingsenhetId: String
    ) {
        //TODO: Hent mottattTid (som er start oppfølgingsperiode på første vedtak, vi må komme tilbake til hva det er ved seinere vedtak i samme periode.
        //TODO: Avsender er en konstant, versjon må hentes fra Docker-image
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)

        if (statistikkPaa) {

            val oppfolgingsperiode = veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr)
            oppfolgingsperiode.ifPresent {
                val mottattTid = if (hentStatistikkRader(oppfolgingsperiode.get().uuid)) {
                    LocalDateTime.now()
                } else {
                    veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr).get().startDato.toLocalDateTime()
                }


                val sakId = veilarboppfolgingClient.hentOppfolgingsperiodeSak(oppfolgingsperiode.get().uuid).sakId
                val sakStatistikk = SakStatistikk(
                    aktorId = aktorId,
                    oppfolgingPeriodeUUID = oppfolgingsperiode.get().uuid,
                    behandlingId = behandlingId.toBigInteger(),
                    sakId = sakId.toString(),
                    mottattTid = mottattTid,
                    endretTid = LocalDateTime.now(),
                    tekniskTid = LocalDateTime.now(),
                    opprettetAv = veilederIdent,
                    ansvarligEnhet = oppfolgingsenhetId,
                    avsender = AVSENDER,
                    versjon = environmentProperties.naisAppImage
                )
                try {
                    sakStatistikkRepository.insertSakStatistikkRad(sakStatistikk)
                    bigQueryService.logEvent(sakStatistikk)
                } catch (e: Exception) {
                    secureLog.error("Kunne ikke lagre sakstatistikk", e)
                }

            }
        }
    }
}


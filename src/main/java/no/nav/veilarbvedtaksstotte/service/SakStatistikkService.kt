package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.domain.statistikk.SakStatistikk
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import io.getunleash.DefaultUnleash
import no.nav.veilarbvedtaksstotte.utils.SAK_STATISTIKK_PAA

private const val AVSENDER = "Oppfølgingsvedtak § 14 a"

@Service
class SakStatistikkService @Autowired constructor(
    private val sakStatistikkRepository: SakStatistikkRepository,
    private val authService: AuthService,
    private val aktorOppslagClient: AktorOppslagClient,
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val unleashClient: DefaultUnleash
) {
    private fun sjekkOmUtkastRadFinnes(statistikkListe: List<SakStatistikk>?): Boolean {
        if (statistikkListe == null) {
            return false
        }
        val antallUtkast = statistikkListe.stream()
            .filter { item: SakStatistikk -> item.behandlingMetode == "UTKAST" }
            .toList()
            .size
        return antallUtkast == 0
    }

    fun hentStatistikkRader(fnr: Fnr): List<SakStatistikk> {
        val aktorId = aktorOppslagClient.hentAktorId(fnr)
        authService.sjekkTilgangTilBrukerOgEnhet(fnr)
        return sakStatistikkRepository.hentSakStatistikkListe(aktorId.toString())
    }

    /*
        fun leggTilStatistikkRad(sakStatistikkRad: SakStatistikk): Boolean {
            val personFnr = authService.getFnrOrThrow(sakStatistikkRad.aktorId)
            val eksisterendeRader = hentStatistikkRader(personFnr)
            log.debug("Eksisterende rader: {}", true)
            if (sjekkOmUtkastRadFinnes(eksisterendeRader)
            ) {
                log.info("Insert SakStatistikk-rad for bruker: {}", sakStatistikkRad)
                sakStatistikkRepository.insertSakStatistikkRad(sakStatistikkRad)
                return true
            }
            return false
        }
    */
    fun leggTilStatistikkRadUtkast(
        behandlingId: Long,
        aktorId: String,
        fnr: Fnr,
        veilederIdent: String,
        oppfolgingsenhetId: String
    ) {
        //TODO: Hent mottattTid (som er start oppfølgingsperiode på første vedtak, vi må komme tilbake til hva det er ved seinere vedtak i samme periode.
        //TODO: Avsender er en konstant, versjon må hentes fra Docker-image
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)
        val mottattTid = if (sjekkOmUtkastRadFinnes(hentStatistikkRader(fnr))) {
            LocalDateTime.now()
        } else {
            veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr).get().startDato.toLocalDateTime()
        }
        if (statistikkPaa) {
            val oppfolgingsperiode = veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr)
            oppfolgingsperiode.ifPresent {
                val sakId = veilarboppfolgingClient.hentOppfolgingsperiodeSak(oppfolgingsperiode.get().uuid).sakId
                val sakStatistikk = SakStatistikk(
                    aktorId = aktorId,
                    oppfolgingPeriodeUUID = oppfolgingsperiode.get().uuid,
                    behandlingId = behandlingId.toBigInteger(),
                    sakId = sakId.toString(),
                    mottattTid = mottattTid,
                    endretTid = LocalDateTime.now(), tekniskTid = LocalDateTime.now(),
                    opprettetAv = veilederIdent,
                    ansvarligEnhet = oppfolgingsenhetId,
                    avsender = AVSENDER, versjon = "Dockerimage_tag_1"
                )
                sakStatistikkRepository.insertSakStatistikkRad(sakStatistikk)
            }
        }
    }


}


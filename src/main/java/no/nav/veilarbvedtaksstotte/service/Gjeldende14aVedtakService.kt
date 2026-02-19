package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.EksternBrukerId
import no.nav.veilarbvedtaksstotte.domain.vedtak.Gjeldende14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.toGjeldende14aVedtak
import no.nav.veilarbvedtaksstotte.repository.SisteOppfolgingPeriodeRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class Gjeldende14aVedtakService(
    @param:Autowired val siste14aVedtakService: Siste14aVedtakService,
    @param:Autowired val sisteOppfolgingPeriodeRepository: SisteOppfolgingPeriodeRepository,
    @param:Autowired val aktorOppslagClient: AktorOppslagClient
) {
    val logger: Logger = LoggerFactory.getLogger(Gjeldende14aVedtakService::class.java)

    fun hentGjeldende14aVedtak(brukerIdent: EksternBrukerId): Gjeldende14aVedtak? {
        val identer: BrukerIdenter = aktorOppslagClient.hentIdenter(brukerIdent)
        val innevaerendeoppfolgingsperiode =
            sisteOppfolgingPeriodeRepository.hentInnevaerendeOppfolgingsperiode(identer.aktorId).also {
                if (it == null) {
                    logger.info(
                        "Fant ingen gjeldende § 14 a-vedtak for personen. Årsak: personen har ingen " + "inneværende oppfølgingsperiode/er ikke under oppfølging."
                    )
                }
            } ?: return null
        val siste14aVedtak = siste14aVedtakService.hentSiste14aVedtak(brukerIdent).also {
            if (it == null) {
                logger.info(
                    "Fant ingen gjeldende § 14 a-vedtak for personen. Årsak: personen har ingen § 14 a-vedtak."
                )
            }
        } ?: return null

        val erGjeldende: Boolean = sjekkOmVedtakErGjeldende(siste14aVedtak, innevaerendeoppfolgingsperiode.startdato)

        return if (erGjeldende) {
            siste14aVedtak.toGjeldende14aVedtak()
        } else {
            logger.info(
                "Fant ingen gjeldende § 14 a-vedtak for personen. Årsak: personen hadde et § 14 a-vedtak fra {} " + "som ble fattet {}, og inneværende oppfølgingsperiode med startdato {}. Vedtaket er derfor ikke " + "gjeldende.",
                if (siste14aVedtak.fraArena) "Arena" else "ny vedtaksløsning",
                siste14aVedtak.fattetDato,
                innevaerendeoppfolgingsperiode.startdato
            )
            null
        }
    }

    companion object {
        val LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE: ZonedDateTime =
            ZonedDateTime.of(2017, 12, 4, 0, 0, 0, 0, ZoneId.systemDefault())

        fun sjekkOmVedtakErGjeldende(
            siste14aVedtakForBruker: Siste14aVedtak, startDatoInnevarendeOppfolgingsperiode: ZonedDateTime
        ): Boolean {
            return if (siste14aVedtakForBruker.fraArena) {
                erVedtakFraArenaGjeldende(siste14aVedtakForBruker, startDatoInnevarendeOppfolgingsperiode)
            } else {
                erVedtakFraVeilarbvedtaksstotteGjeldende(
                    siste14aVedtakForBruker, startDatoInnevarendeOppfolgingsperiode
                )
            }
        }

        private fun erVedtakFraArenaGjeldende(
            siste14aVedtakForBruker: Siste14aVedtak, startDatoInnevarendeOppfolgingsperiode: ZonedDateTime
        ): Boolean {
            // 2025-02-18
            // Vi har oppdaget at vedtak fattet i Arena får "fattetDato" lik midnatt den dagen vedtaket ble fattet.
            // Derfor har vi valgt å innfør en "grace periode" på 4 døgn. Dvs. dersom vedtaket ble fattet etter
            // "oppfølgingsperiode startdato - 4 døgn", så anser vi det som gjeldende.

            val erVedtaketFattetIInnevarendeOppfolgingsperiodeMedGracePeriodePa4Dogn =
                siste14aVedtakForBruker.fattetDato.isAfter(startDatoInnevarendeOppfolgingsperiode.minusDays(4))
            val erVedtaketFattetForLanseringsdatoForVeilarboppfolging =
                siste14aVedtakForBruker.fattetDato.isBefore(LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE)
            val erStartdatoForOppfolgingsperiodeLikLanseringsdatoForVeilarboppfolging =
                !startDatoInnevarendeOppfolgingsperiode.isAfter(LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE)

            return erVedtaketFattetIInnevarendeOppfolgingsperiodeMedGracePeriodePa4Dogn || (erVedtaketFattetForLanseringsdatoForVeilarboppfolging && erStartdatoForOppfolgingsperiodeLikLanseringsdatoForVeilarboppfolging)
        }

        private fun erVedtakFraVeilarbvedtaksstotteGjeldende(
            siste14aVedtakForBruker: Siste14aVedtak, startDatoInnevarendeOppfolgingsperiode: ZonedDateTime
        ): Boolean {
            return siste14aVedtakForBruker.fattetDato.isAfter(startDatoInnevarendeOppfolgingsperiode)
        }
    }
}

package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.EksternBrukerId
import no.nav.veilarbvedtaksstotte.domain.vedtak.Gjeldende14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class Gjeldende14aVedtakService(
    @Autowired
    val siste14aVedtakService: Siste14aVedtakService
) {

    fun hentGjeldende14aVedtak(brukerIdent: EksternBrukerId): Gjeldende14aVedtak? {
        TODO()
    }

    companion object {
        private val LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE: ZonedDateTime =
            ZonedDateTime.of(2017, 12, 4, 0, 0, 0, 0, ZoneId.systemDefault())

        fun sjekkOmVedtakErGjeldende(
            siste14aVedtakForBruker: Siste14aVedtak,
            startDatoInnevarendeOppfolgingsperiode: ZonedDateTime
        ): Boolean {
            // 2025-02-18
            // Vi har oppdaget at vedtak fattet i Arena får "fattetDato" lik midnatt den dagen vedtaket ble fattet.
            // Derfor har vi valgt å innfør en "grace periode" på 4 døgn. Dvs. dersom vedtaket ble fattet etter
            // "oppfølgingsperiode startdato - 4 døgn", så anser vi det som gjeldende.
            val erVedtaketFattetIInnevarendeOppfolgingsperiodeMedGracePeriodePa4Dogn =
                siste14aVedtakForBruker.fattetDato.isAfter(startDatoInnevarendeOppfolgingsperiode.minusDays(4))
            val erVedtaketFattetForLanseringsdatoForVeilarboppfolging = siste14aVedtakForBruker.fattetDato
                .isBefore(LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE)
            val erStartdatoForOppfolgingsperiodeLikLanseringsdatoForVeilarboppfolging =
                !startDatoInnevarendeOppfolgingsperiode
                    .isAfter(LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE)

            return erVedtaketFattetIInnevarendeOppfolgingsperiodeMedGracePeriodePa4Dogn ||
                    (erVedtaketFattetForLanseringsdatoForVeilarboppfolging
                            && erStartdatoForOppfolgingsperiodeLikLanseringsdatoForVeilarboppfolging)
        }
    }
}

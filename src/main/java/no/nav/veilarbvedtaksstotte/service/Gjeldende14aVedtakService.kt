package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.vedtak.Gjeldende14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class Gjeldende14aVedtakService {

    /*
        "Initiell datalast - engangsjobb"

        1. Lag et database view (identer_i_veilarbvedtaksstotte) som inneholder alle identer vi har minst et vedtak på

        2. Konfigurere og deploye nytt topic: obo.oppfolgingsvedtak-14a.

        3. Ta alle eksisterende vedtak (vedtak + arena_vedtak), finne hvilke som er gjeldende, og publisere disse på
        obo.oppfolgingsvedtak-14a
            1.1 Hent <batch_size> identer fra "identer_i_veilarbvedtaksstotte"
            1.2 For alle identer hent siste § 14 a-vedtak
            1.3 For alle siste § 14 a-vedtak
                1.3.1 Sjekk om vedtaket er gjeldende
                    1.3.1.a Dersom ja: putt melding i "utboks" (kafka_producer_record)
                    1.3.1.b Dersom nei: gjør ingenting, fortsett

        "Daglig oppførsel"

     */

    fun hentGjeldende14aVedtak(brukerIdent: AktorId): Gjeldende14aVedtak? {

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
package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.EksternBrukerId
import no.nav.veilarbvedtaksstotte.domain.vedtak.Gjeldende14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.toGjeldende14aVedtak
import no.nav.veilarbvedtaksstotte.repository.SisteOppfolgingPeriodeRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class Gjeldende14aVedtakService (
    @Autowired val siste14aVedtakService: Siste14aVedtakService,
    @Autowired val sisteOppfolgingPeriodeRepository: SisteOppfolgingPeriodeRepository,
    @Autowired val aktorOppslagClient: AktorOppslagClient
) {

    fun hentGjeldende14aVedtak(brukerIdent: EksternBrukerId): Gjeldende14aVedtak? {
        val identer: BrukerIdenter = aktorOppslagClient.hentIdenter(brukerIdent)
        val innevarendeoppfolgingsperiode = sisteOppfolgingPeriodeRepository.hentInnevarendeOppfolgingsperiode(identer.aktorId) ?: return null
        val siste14aVedtak = siste14aVedtakService.siste14aVedtak(brukerIdent) ?: return null

        val erGjeldende: Boolean = sjekkOmVedtakErGjeldende(siste14aVedtak, innevarendeoppfolgingsperiode.startdato)

        return if (erGjeldende) {
            siste14aVedtak.toGjeldende14aVedtak() //PS 2025-03-12 dette objektet har ikke vedtakId :) Fortsettelse følger
        } else {
            null
        }
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

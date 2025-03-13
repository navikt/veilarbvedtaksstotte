package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.EksternBrukerId
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Gjeldende14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.toGjeldende14aVedtak
import no.nav.veilarbvedtaksstotte.repository.SisteOppfolgingPeriodeRepository
import no.nav.veilarbvedtaksstotte.utils.SecureLog
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class Gjeldende14aVedtakService(
    @Autowired val siste14aVedtakService: Siste14aVedtakService,
    @Autowired val sisteOppfolgingPeriodeRepository: SisteOppfolgingPeriodeRepository,
    @Autowired val aktorOppslagClient: AktorOppslagClient,
    @Autowired val kafkaProducerService: KafkaProducerService
) {

    val logger: Logger = LoggerFactory.getLogger(Gjeldende14aVedtakService::class.java)

    fun behandleGjeldende14aVedtak(arenaVedtak: ArenaVedtak) {
        val gjeldende14aVedtak = hentGjeldende14aVedtak(arenaVedtak.fnr)
        val aktorId = aktorOppslagClient.hentIdenter(arenaVedtak.fnr).aktorId

        if (gjeldende14aVedtak == null) {
            kafkaProducerService.sendGjeldende14aVedtak(aktorId, null)
            logger.info("Fant ingen gjeldende § 14 a-vedtak for person. Sendte tombstone-melding.")
            SecureLog.secureLog.info(
                "Fant ingen gjeldende § 14 a-vedtak for person med Aktør-ID {}. Sendte tombstone-melding.",
                aktorId.get()
            )
        }

        if (arenaVedtak.vedtakId.toString() == gjeldende14aVedtak?.vedtakId) {
            kafkaProducerService.sendGjeldende14aVedtak(aktorId, gjeldende14aVedtak.toGjeldende14aVedtakKafkaDTO())
            logger.info("Mottat § 14 a-vedtak fra Arena er nytt gjeldende vedtak for person. Sender melding om gjeldende § 14 a-vedtak.")
            SecureLog.secureLog.info(
                "Mottat § 14 a-vedtak fra Arena er nytt gjeldende vedtak for person med Aktør-ID {}. Sender melding om gjeldende § 14 a-vedtak.",
                aktorId.get()
            )
        }
    }

    fun hentGjeldende14aVedtak(brukerIdent: EksternBrukerId): Gjeldende14aVedtak? {
        val identer: BrukerIdenter = aktorOppslagClient.hentIdenter(brukerIdent)
        val innevarendeoppfolgingsperiode =
            sisteOppfolgingPeriodeRepository.hentInnevarendeOppfolgingsperiode(identer.aktorId) ?: return null
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

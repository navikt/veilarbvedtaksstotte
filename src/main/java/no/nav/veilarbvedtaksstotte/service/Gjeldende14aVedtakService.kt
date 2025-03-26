package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EksternBrukerId
import no.nav.veilarbvedtaksstotte.domain.oppfolgingsperiode.SisteOppfolgingsperiode
import no.nav.veilarbvedtaksstotte.domain.vedtak.Gjeldende14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.toGjeldende14aVedtak
import no.nav.veilarbvedtaksstotte.repository.BrukerIdenterRepository
import no.nav.veilarbvedtaksstotte.repository.SisteOppfolgingPeriodeRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class Gjeldende14aVedtakService(
    @Autowired val siste14aVedtakService: Siste14aVedtakService,
    @Autowired val sisteOppfolgingPeriodeRepository: SisteOppfolgingPeriodeRepository,
    @Autowired val aktorOppslagClient: AktorOppslagClient,
    @Autowired val brukerIdenterRepository: BrukerIdenterRepository,
    @Autowired val kafkaProducerService: KafkaProducerService,
    @Autowired val leaderElectionClient: LeaderElectionClient
) {

    val logger: Logger = LoggerFactory.getLogger(Gjeldende14aVedtakService::class.java)

    @Scheduled
    fun utforDatalast() {
        if(leaderElectionClient.isLeader) {
            val antallPersonerUnderOppfolging: Int = sisteOppfolgingPeriodeRepository.hentAntallPersonUnderOppfolging()
            var currentBatchOffset = 0
            val batchStorrelse = 100

            while (currentBatchOffset < antallPersonerUnderOppfolging) {
                try {
                    // Hent 100 personer under oppfølging
                    val aktorIdSisteOppfolgingsperiodeMap: Map<AktorId, SisteOppfolgingsperiode> =
                        sisteOppfolgingPeriodeRepository.hentPersonerUnderOppfolging(currentBatchOffset, batchStorrelse)
                            .associateBy { it.aktorId }

                    // For hver person under oppfølging
                    aktorIdSisteOppfolgingsperiodeMap.forEach {
                        // Hent gjeldende § 14 a-vedtak for personen, dersom hen har et
                        val personNokkel = brukerIdenterRepository.hentTilknyttetPerson(it.key)
                        val gjeldende14aVedtak: Gjeldende14aVedtak? =
                            hentGjeldende14aVedtak(
                                brukerIdent = it.key,
                                aktorIdSupplier = { brukerIdenterRepository.hentAktiveIdenter(personNokkel).aktorId }
                            )

                        kafkaProducerService.sendGjeldende14aVedtak(it.key, gjeldende14aVedtak)
                    }

                    // Inkrementer currentBatchOffset
                    currentBatchOffset += batchStorrelse
                } catch (e: RuntimeException) {
                    // Job failed
                    break
                }
            }
        }
    }

    fun hentGjeldende14aVedtak(
        brukerIdent: EksternBrukerId,
        aktorIdSupplier: ((brukerIdent: EksternBrukerId) -> AktorId)? = null
    ): Gjeldende14aVedtak? {
        val aktorId = aktorIdSupplier?.invoke(brukerIdent) ?: aktorOppslagClient.hentIdenter(brukerIdent).aktorId
        val innevaerendeoppfolgingsperiode =
            sisteOppfolgingPeriodeRepository.hentInnevaerendeOppfolgingsperiode(aktorId).also {
                if (it == null) {
                    logger.info(
                        "Fant ingen gjeldende § 14 a-vedtak for personen. Årsak: personen har ingen " +
                                "inneværende oppfølgingsperiode/er ikke under oppfølging."
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
                "Fant ingen gjeldende § 14 a-vedtak for personen. Årsak: personen hadde et § 14 a-vedtak fra {} " +
                        "som ble fattet {}, og inneværende oppfølgingsperiode med startdato {}. Vedtaket er derfor ikke " +
                        "gjeldende.",
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
            siste14aVedtakForBruker: Siste14aVedtak,
            startDatoInnevarendeOppfolgingsperiode: ZonedDateTime
        ): Boolean {
            return if (siste14aVedtakForBruker.fraArena) {
                erVedtakFraArenaGjeldende(siste14aVedtakForBruker, startDatoInnevarendeOppfolgingsperiode)
            } else {
                erVedtakFraVeilarbvedtaksstotteGjeldende(
                    siste14aVedtakForBruker,
                    startDatoInnevarendeOppfolgingsperiode
                )
            }
        }

        private fun erVedtakFraArenaGjeldende(
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

        private fun erVedtakFraVeilarbvedtaksstotteGjeldende(
            siste14aVedtakForBruker: Siste14aVedtak,
            startDatoInnevarendeOppfolgingsperiode: ZonedDateTime
        ): Boolean {
            return siste14aVedtakForBruker.fattetDato.isAfter(startDatoInnevarendeOppfolgingsperiode)
        }
    }
}

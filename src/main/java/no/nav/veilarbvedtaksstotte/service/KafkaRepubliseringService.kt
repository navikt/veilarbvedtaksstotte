package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KafkaRepubliseringService(
    val vedtaksstotteRepository: VedtaksstotteRepository,
    val arenaVedtakRepository: ArenaVedtakRepository,
    val siste14aVedtakService: Siste14aVedtakService
) {

    val log: Logger = LoggerFactory.getLogger(KafkaRepubliseringService::class.java)

    /**
     * Republiserer siste 14a vedtak for brukere som har fått vedtak i vedtaksstøtte (denne løsningen). Republiserer
     * ikke for brukere som bare har vedtak i Arena, men dersom en bruker har vedtak i denne løsningen og et nyere i
     * Arena, så vil vedtak fra Arena bli republisert.
     */
    fun republiserSiste14aVedtakFraVedtaksstotte() {

            val unikeAktorIder = vedtaksstotteRepository.hentUnikeBrukereMedFattetVedtakPage()

            log.info(
                "Republiserer siste 14a vedtak for alle brukere som har vedtak i vedtaksstøtte. Antall brukere={}",
                unikeAktorIder.size
            )

            unikeAktorIder.forEach { aktorId -> siste14aVedtakService.republiserKafkaSiste14aVedtak(aktorId) }
    }

    fun republiserSiste14aVedtak() {

        val brukereFraVedtaksstotte = vedtaksstotteRepository.hentUnikeBrukereMedFattetVedtakPage()
        val brukereFraArena = arenaVedtakRepository.hentUnikeBrukereMedVedtak()

        val alleBrukere = brukereFraVedtaksstotte + brukereFraArena

        log.info(
            "Republiserer siste 14a vedtak for alle brukere som har vedtak i vedtaksstøtte og Arena." +
                    " Antall brukere i vedtaksstøtte=${brukereFraVedtaksstotte.size}" +
                    " Antall brukere i Arena=${brukereFraArena.size}"
        )

        alleBrukere.forEach { aktorId -> siste14aVedtakService.republiserKafkaSiste14aVedtak(aktorId) }
    }
}

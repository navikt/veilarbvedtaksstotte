package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KafkaRepubliseringService(
    val vedtaksstotteRepository: VedtaksstotteRepository,
    val innsatsbehovService: InnsatsbehovService
) {

    val log: Logger = LoggerFactory.getLogger(KafkaRepubliseringService::class.java)

    val pageSize = 1000;

    /**
     * Republiserer innsatsbehov for brukere som har fått vedtak i vedtaksstøtte (denne løsningen). Republiserer ikke
     * for brukere som bare har vedtak i Arena, men dersom en bruker har vedtak i denne løsningen og et nyere i Arena,
     * så vil innsatsbehovet fra vedtak i Arena bli republisert.
     */
    fun republiserInnsatsbehovVedtaksstotte() {
        var currentOffset = 0;

        while (true) {
            val unikeAktorIder = vedtaksstotteRepository.hentUnikeBrukereMedFattetVedtakPage(currentOffset, pageSize);

            if (unikeAktorIder.isEmpty()) {
                break;
            }

            currentOffset += unikeAktorIder.size;

            log.info(
                "Republiserer innsatsbehov for brukere som har vedtak i vedtaksstøtte. CurrentOffset={} BatchSize={}",
                currentOffset,
                unikeAktorIder.size
            )

            unikeAktorIder.forEach { aktorId -> innsatsbehovService.republiserKafkaInnsatsbehov(aktorId) }
        }
    }
}

package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.config.KafkaProperties
import no.nav.veilarbvedtaksstotte.controller.dto.RepubliserVedtakPaKafkaTopicRequest
import no.nav.veilarbvedtaksstotte.domain.kafka.toKafkaVedtakSendt
import no.nav.veilarbvedtaksstotte.domain.vedtak.toSiste14aVedtak
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KafkaRepubliseringService(
    val vedtaksstotteRepository: VedtaksstotteRepository,
    val arenaVedtakRepository: ArenaVedtakRepository,
    val siste14aVedtakService: Siste14aVedtakService,
    val dvhRapporteringService: DvhRapporteringService,
    val kafkaProducerService: KafkaProducerService,
    val kafkaProperties: KafkaProperties
) {

    val log: Logger = LoggerFactory.getLogger(KafkaRepubliseringService::class.java)

    fun republiserSiste14aVedtak() {

        val brukereFraVedtaksstotte = vedtaksstotteRepository.hentUnikeBrukereMedFattetVedtak()
        val brukereFraArena = arenaVedtakRepository.hentUnikeBrukereMedVedtak()

        val alleBrukere = brukereFraVedtaksstotte + brukereFraArena

        log.info(
            "Republiserer siste 14a vedtak for alle brukere som har vedtak i vedtaksstøtte og Arena." +
                    " Antall brukere i vedtaksstøtte=${brukereFraVedtaksstotte.size}" +
                    " Antall brukere i Arena=${brukereFraArena.size}"
        )

        alleBrukere.forEach { aktorId -> siste14aVedtakService.republiserKafkaSiste14aVedtak(aktorId) }
    }

    fun republiserVedtak14aFattetDvh(batchSize: Int) {
        log.info("Republiserer alle fattede vedtak på dvh-topic.")
        var offset = 0
        do {
            val batch = vedtaksstotteRepository.hentFattedeVedtak(batchSize, offset)
            log.info("Rebubliserer ${batch.size} vedtak, hentet med offset $offset")
            batch.forEach { dvhRapporteringService.produserVedtakFattetDvhMelding(it) }
            offset += batch.size
        } while (batch.size == batchSize)

    }

    fun republiserVedtakPaKafkaTopic(request: RepubliserVedtakPaKafkaTopicRequest) {
        log.info("Republiserer vedtak med ider ${request.vedtaksIDer} på kafka topics ${request.kafkaTopic}")
        request.vedtaksIDer.forEach { vedtakId ->
            val vedtak = vedtaksstotteRepository.hentVedtak(vedtakId.toLong())
            if (vedtak != null) {
                when (request.kafkaTopic) {
                    kafkaProperties.siste14aVedtakTopic -> kafkaProducerService.sendSiste14aVedtak(vedtak.toSiste14aVedtak())
                    kafkaProperties.vedtakSendtTopic -> kafkaProducerService.sendVedtakSendt(vedtak.toKafkaVedtakSendt())
                    else -> return
                }
            } else {
                log.warn("Fant ikke vedtak med id $vedtakId")
            }
        }
    }
}

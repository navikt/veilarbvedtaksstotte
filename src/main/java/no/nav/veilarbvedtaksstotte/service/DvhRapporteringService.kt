package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring
import no.nav.veilarbvedtaksstotte.domain.kafka.VedtakStatusEndring.VEDTAK_SENDT
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.springframework.stereotype.Service

@Service
class DvhRapporteringService(
    private val vedtaksstotteRepository: VedtaksstotteRepository,
    private val kafkaProducerService: KafkaProducerService
) {

    fun rapporterTilDvh(statusEndring: KafkaVedtakStatusEndring) {
        if (statusEndring.vedtakStatusEndring == VEDTAK_SENDT) {
            val vedtak = vedtaksstotteRepository.hentVedtak(statusEndring.vedtakId)

            kafkaProducerService.sendVedtakFattetDvh(vedtak)
        }
    }
}

package no.nav.veilarbvedtaksstotte.service

import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage
import no.nav.common.kafka.producer.util.ProducerUtils
import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.config.KafkaProperties
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakSendt
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring
import no.nav.veilarbvedtaksstotte.domain.vedtak.Gjeldende14aVedtakKafkaDTO
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Service

@Service
class KafkaProducerService(
    private val producerRecordStorage: KafkaProducerRecordStorage,
    private val kafkaProperties: KafkaProperties
) {
    fun sendVedtakStatusEndring(vedtakStatusEndring: KafkaVedtakStatusEndring) {
        val producerRecord =
            ProducerUtils.serializeJsonRecord(
                ProducerRecord(
                    kafkaProperties.vedtakStatusEndringTopic,
                    vedtakStatusEndring.aktorId,
                    vedtakStatusEndring
                )
            )

        producerRecordStorage.store(producerRecord)
    }

    fun sendVedtakSendt(vedtakSendt: KafkaVedtakSendt) {
        val producerRecord =
            ProducerUtils.serializeJsonRecord(
                ProducerRecord(
                    kafkaProperties.vedtakSendtTopic,
                    vedtakSendt.aktorId,
                    vedtakSendt
                )
            )

        producerRecordStorage.store(producerRecord)
    }

    fun sendSiste14aVedtak(siste14aVedtak: Siste14aVedtak?) {
        val producerRecord =
            ProducerUtils.serializeJsonRecord(
                ProducerRecord(
                    kafkaProperties.siste14aVedtakTopic,
                    siste14aVedtak?.aktorId?.get(),
                    siste14aVedtak
                )
            )

        producerRecordStorage.store(producerRecord)
    }

    fun sendGjeldende14aVedtak(aktorId: AktorId, gjeldende14aVedtakKafkaDto: Gjeldende14aVedtakKafkaDTO?) {
        val producerRecord =
            ProducerUtils.serializeJsonRecord(
                ProducerRecord(
                    kafkaProperties.gjeldende14aVedtakTopic,
                    aktorId.get(),
                    gjeldende14aVedtakKafkaDto
                )
            )

        producerRecordStorage.store(producerRecord)
    }
}
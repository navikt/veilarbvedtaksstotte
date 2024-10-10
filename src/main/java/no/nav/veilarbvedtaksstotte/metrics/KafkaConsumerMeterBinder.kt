package no.nav.veilarbvedtaksstotte.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import no.nav.veilarbvedtaksstotte.repository.KafkaConsumerRecordRepository

import org.springframework.stereotype.Component

@Component
class KafkaConsumerMeterBinder(
    val kafkaConsumerRecordRepository: KafkaConsumerRecordRepository
) : MeterBinder {

    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder("antall_rader_i_kafka_consumer_record_over_retriesgrense") {
            antallRaderIKafkaConsumerRecordOverRetriesgrense()
        }.description("Antall rader").register(registry)
    }

    fun antallRaderIKafkaConsumerRecordOverRetriesgrense(): Int {
        return kafkaConsumerRecordRepository.hentAntallRaderIKafkaConsumerRecordOverRetriesgrense()
    }
}

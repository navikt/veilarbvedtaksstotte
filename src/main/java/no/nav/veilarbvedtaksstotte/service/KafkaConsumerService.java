package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.IdUtils;
import no.nav.person.pdl.aktor.v2.Aktor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaConsumerService {

    private final BrukerIdenterService brukerIdenterService;

    private static final String MDC_KAFKA_CONSUMER_SERVICE_CORRELATION_ID_KEY = "kafka_consumer_correlation_id";

    @Autowired
    public KafkaConsumerService(
            BrukerIdenterService brukerIdenterService
    ) {
        this.brukerIdenterService = brukerIdenterService;
    }

    public <K, V> void behandleKafkaMelding(ConsumerRecord<K, V> melding, KafkaMeldingBehandler<K, V> meldingBehandler) {
        MDC.put(MDC_KAFKA_CONSUMER_SERVICE_CORRELATION_ID_KEY, IdUtils.generateId());
        try {
            log.info("Behandler Kafka-melding. Topic: {}, offset: {}, partisjon: {}.", melding.topic(), melding.offset(), melding.partition());
            meldingBehandler.behandleMelding(melding);
        } finally {
            MDC.remove(MDC_KAFKA_CONSUMER_SERVICE_CORRELATION_ID_KEY);
        }
    }

    public void behandlePdlAktorV2Melding(ConsumerRecord<String, Aktor> aktorRecord) {
        brukerIdenterService.behandlePdlAktorV2Melding(aktorRecord);
    }
}
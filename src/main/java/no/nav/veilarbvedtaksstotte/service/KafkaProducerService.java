package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    final KafkaProducerRecordStorage<String, String> producerRecordStorage;

    public KafkaProducerService(KafkaProducerRecordStorage<String, String> producerRecordStorage) {
        this.producerRecordStorage = producerRecordStorage;
    }
}

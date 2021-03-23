package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.veilarbvedtaksstotte.config.KafkaProperties;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaVedtakStatusEndring;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;

import static no.nav.common.kafka.producer.util.ProducerUtils.toJsonProducerRecord;

@Service
public class KafkaProducerService {
    final KafkaProducerRecordStorage<String, String> producerRecordStorage;
    final KafkaProperties kafkaProperties;

    public KafkaProducerService(KafkaProducerRecordStorage<String, String> producerRecordStorage, KafkaProperties kafkaProperties) {
        this.producerRecordStorage = producerRecordStorage;
        this.kafkaProperties = kafkaProperties;
    }

    public void sendVedtakStatusEndring(KafkaVedtakStatusEndring vedtakStatusEndring) {
        ProducerRecord<String, String> record =
                toJsonProducerRecord(
                        kafkaProperties.getVedtakStatusEndringTopic(),
                        vedtakStatusEndring.getAktorId(),
                        vedtakStatusEndring
                );

        producerRecordStorage.store(record);
    }

    public void sendVedtakSendt(KafkaVedtakSendt vedtakSendt) {
        ProducerRecord<String, String> record =
                toJsonProducerRecord(
                        kafkaProperties.getVedtakSendtTopic(),
                        vedtakSendt.getAktorId(),
                        vedtakSendt
                );

        producerRecordStorage.store(record);
    }
}

package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.veilarbvedtaksstotte.config.KafkaProperties;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;

import static no.nav.common.kafka.producer.util.ProducerUtils.*;

@Service
public class KafkaProducerService {
    final KafkaProducerRecordStorage producerRecordStorage;
    final KafkaProperties kafkaProperties;

    public KafkaProducerService(
            KafkaProducerRecordStorage producerRecordStorage,
            KafkaProperties kafkaProperties
    ) {
        this.producerRecordStorage = producerRecordStorage;
        this.kafkaProperties = kafkaProperties;
    }

    public void sendVedtakStatusEndring(KafkaVedtakStatusEndring vedtakStatusEndring) {
        ProducerRecord<byte[], byte[]> producerRecord =
                serializeJsonRecord(
                        new ProducerRecord<>(
                                kafkaProperties.getVedtakStatusEndringTopic(),
                                vedtakStatusEndring.getAktorId(),
                                vedtakStatusEndring));

        producerRecordStorage.store(producerRecord);
    }

    public void sendVedtakSendt(KafkaVedtakSendt vedtakSendt) {
        ProducerRecord<byte[], byte[]> producerRecord =
                serializeJsonRecord(
                        new ProducerRecord<>(
                                kafkaProperties.getVedtakSendtTopic(),
                                vedtakSendt.getAktorId(),
                                vedtakSendt));

        producerRecordStorage.store(producerRecord);
    }

    public void sendInnsatsbehov(Innsatsbehov innsatsbehov) {
        ProducerRecord<byte[], byte[]> producerRecord =
                serializeJsonRecord(
                        new ProducerRecord<>(
                                kafkaProperties.getInnsatsbehovTopic(),
                                innsatsbehov.getAktorId().get(),
                                innsatsbehov));

        producerRecordStorage.store(producerRecord);
    }
}

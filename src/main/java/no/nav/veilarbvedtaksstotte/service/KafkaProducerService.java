package no.nav.veilarbvedtaksstotte.service;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.pto_schema.kafka.avro.Vedtak14aFattetDvh;
import no.nav.veilarbvedtaksstotte.config.KafkaConfig;
import no.nav.veilarbvedtaksstotte.config.KafkaProperties;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;

import static no.nav.common.kafka.producer.util.ProducerUtils.serializeJsonRecord;
import static no.nav.common.kafka.producer.util.ProducerUtils.serializeRecord;

@Slf4j
@Service
public class KafkaProducerService {
    private final KafkaProducerRecordStorage producerRecordStorage;
    private final KafkaProperties kafkaProperties;
    private final KafkaAvroSerializer kafkaAvroSerializer;

    public KafkaProducerService(KafkaProducerRecordStorage producerRecordStorage,
                                KafkaProperties kafkaProperties,
                                KafkaConfig.KafkaAvroContext kafkaAvroContext) {
        this.producerRecordStorage = producerRecordStorage;
        this.kafkaProperties = kafkaProperties;
        this.kafkaAvroSerializer = new KafkaAvroSerializer(null, kafkaAvroContext.getConfig());
    }

    public void sendVedtakStatusEndring(KafkaVedtakStatusEndring vedtakStatusEndring) {
        ProducerRecord<byte[], byte[]> producerRecordOnPrem =
                serializeJsonRecord(
                        new ProducerRecord<>(
                                kafkaProperties.getVedtakStatusEndringOnPremTopic(),
                                vedtakStatusEndring.getAktorId(),
                                vedtakStatusEndring));

        ProducerRecord<byte[], byte[]> producerRecord =
                serializeJsonRecord(
                        new ProducerRecord<>(
                                kafkaProperties.getVedtakStatusEndringTopic(),
                                vedtakStatusEndring.getAktorId(),
                                vedtakStatusEndring));

        producerRecordStorage.store(producerRecordOnPrem);
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

    public void sendSiste14aVedtak(Siste14aVedtak siste14aVedtak) {
        ProducerRecord<byte[], byte[]> producerRecord =
                serializeJsonRecord(
                        new ProducerRecord<>(
                                kafkaProperties.getSiste14aVedtakTopic(),
                                siste14aVedtak.getAktorId().get(),
                                siste14aVedtak));

        producerRecordStorage.store(producerRecord);
    }

    public void sendVedtakFattetDvh(Vedtak14aFattetDvh vedtak14aFattetDvh) {
        ProducerRecord<byte[], byte[]> producerRecord = null;
        try {
            producerRecord = serializeAvroRecord(
                    new ProducerRecord<>(
                            kafkaProperties.getVedtakFattetDvhTopic(),
                            vedtak14aFattetDvh.getAktorId().toString(),
                            vedtak14aFattetDvh
                    )
            );
        } catch (Exception e) {
            log.error("Klarte ikke serialisere Avro-melding", e);
        }

        if (producerRecord != null) {
            producerRecordStorage.store(producerRecord);
        }
    }

    private  ProducerRecord<byte[], byte[]> serializeAvroRecord(ProducerRecord<String, Object> producerRecord) {
        return serializeRecord(producerRecord, new StringSerializer(), kafkaAvroSerializer);
    }
}

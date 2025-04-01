package no.nav.veilarbvedtaksstotte.service;

import io.getunleash.DefaultUnleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarbvedtaksstotte.config.KafkaProperties;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Gjeldende14aVedtakKafkaDTO;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtakKafkaDTO;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;

import static no.nav.common.kafka.producer.util.ProducerUtils.serializeJsonRecord;
import static no.nav.veilarbvedtaksstotte.utils.UnleashUtilsKt.PRODUSER_OBO_GJELDENDE_14A_VEDTAK_MELDINGER_SKRUDD_PAA;

@Slf4j
@Service
public class KafkaProducerService {
    private final KafkaProducerRecordStorage producerRecordStorage;
    private final KafkaProperties kafkaProperties;
    private final DefaultUnleash unleashService;

    public KafkaProducerService(
            KafkaProducerRecordStorage producerRecordStorage,
            KafkaProperties kafkaProperties,
            DefaultUnleash unleashService
    ) {
        this.producerRecordStorage = producerRecordStorage;
        this.kafkaProperties = kafkaProperties;
        this.unleashService = unleashService;
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

    public void sendSiste14aVedtak(Siste14aVedtakKafkaDTO siste14aVedtakKafkaDTO) {
        ProducerRecord<byte[], byte[]> producerRecord =
                serializeJsonRecord(
                        new ProducerRecord<>(
                                kafkaProperties.getSiste14aVedtakTopic(),
                                siste14aVedtakKafkaDTO.getAktorId().get(),
                                siste14aVedtakKafkaDTO));

        producerRecordStorage.store(producerRecord);
    }

    public void sendGjeldende14aVedtak(AktorId key, Gjeldende14aVedtakKafkaDTO value) {
        if (unleashService.isEnabled(PRODUSER_OBO_GJELDENDE_14A_VEDTAK_MELDINGER_SKRUDD_PAA)) {
            ProducerRecord<byte[], byte[]> producerRecord = serializeJsonRecord(
                    new ProducerRecord<>(
                            kafkaProperties.getGjeldende14aVedtakTopic(),
                            key.get(),
                            value
                    )
            );

            producerRecordStorage.store(producerRecord);
        }
    }
}

package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarbvedtaksstotte.config.KafkaProperties;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;

import static no.nav.common.kafka.producer.util.ProducerUtils.toJsonProducerRecord;
import static no.nav.common.kafka.producer.util.ProducerUtils.toProducerRecord;
import static no.nav.veilarbvedtaksstotte.utils.JsonUtilsKt.toJson;

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

    public void slettInnsatsbehov(AktorId aktorId) {
        sendInnsatsbehov(aktorId, null);
    }

    public void sendInnsatsbehov(Innsatsbehov innsatsbehov) {
        sendInnsatsbehov(innsatsbehov.getAktorId(), innsatsbehov);
    }

    private void sendInnsatsbehov(AktorId aktorId, Innsatsbehov innsatsbehov) {
        String json = innsatsbehov == null ? null : toJson(innsatsbehov);
        ProducerRecord<String, String> producerRecord =
                toProducerRecord(
                        kafkaProperties.getInnsatsbehovTopic(),
                        aktorId.get(),
                        json
                );

        producerRecordStorage.store(producerRecord);
    }
}

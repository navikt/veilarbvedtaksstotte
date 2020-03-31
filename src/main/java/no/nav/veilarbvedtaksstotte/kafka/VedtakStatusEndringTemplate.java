package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.FeiletKafkaMelding;
import no.nav.veilarbvedtaksstotte.domain.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.enums.KafkaTopic;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import org.springframework.kafka.core.KafkaTemplate;

import javax.inject.Inject;

import static no.nav.json.JsonUtils.toJson;
import static no.nav.veilarbvedtaksstotte.config.KafkaProducerConfig.KAFKA_TOPIC_VEDTAK_STATUS_ENDRING;

@Slf4j
public class VedtakStatusEndringTemplate {

    private final String topic;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaRepository kafkaRepository;

    @Inject
    public VedtakStatusEndringTemplate(KafkaTemplate<String, String> kafkaTemplate, String topic, KafkaRepository kafkaRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.kafkaRepository = kafkaRepository;
    }

    public void send(KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        final String serialisertData = toJson(kafkaVedtakStatusEndring);
        kafkaTemplate.send(topic, kafkaVedtakStatusEndring.getAktorId(), serialisertData)
                .addCallback(
                        sendResult -> {},
                        throwable -> onError(throwable, kafkaVedtakStatusEndring.getAktorId(), serialisertData)
                );
    }

    public void sendTidligereFeilet(FeiletKafkaMelding feiletKafkaMelding) {
        kafkaTemplate.send(topic, feiletKafkaMelding.getKey(), feiletKafkaMelding.getJsonPayload())
                .addCallback(
                        sendResult -> onSuccess(feiletKafkaMelding),
                        throwable -> onError(throwable)
                );
    }

    private void onSuccess(FeiletKafkaMelding feiletKafkaMelding) {
        kafkaRepository.slettFeiletKafkaMelding(feiletKafkaMelding.getId());
    }

    private void onError(Throwable throwable) {
        log.error("Kunne ikke publisere tidligere feilet melding på topic: " + KAFKA_TOPIC_VEDTAK_STATUS_ENDRING + "\nERROR: " + throwable);
    }

    private void onError(Throwable throwable, String key, String jsonPayload) {
        log.error("Kunne ikke publisere tidligere feilet melding på topic: " + KAFKA_TOPIC_VEDTAK_STATUS_ENDRING + "\nERROR: " + throwable);
        kafkaRepository.lagreFeiletKafkaMelding(KafkaTopic.VEDTAK_STATUS_ENDRING, key, jsonPayload);
    }

}

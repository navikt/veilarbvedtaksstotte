package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.kafka.dto.FeiletKafkaMelding;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.enums.KafkaTopic;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;

@Slf4j
@Component
public class KafkaProducer {

    private final KafkaTopics kafkaTopics;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final KafkaRepository kafkaRepository;

    @Autowired
    public KafkaProducer(KafkaTopics kafkaTopics, KafkaTemplate<String, String> kafkaTemplate, KafkaRepository kafkaRepository) {
        this.kafkaTopics = kafkaTopics;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaRepository = kafkaRepository;
    }

    public void sendVedtakStatusEndring(KafkaVedtakStatusEndring vedtakStatusEndring) {
        send(KafkaTopic.VEDTAK_STATUS_ENDRING, vedtakStatusEndring.getAktorId(), toJson(vedtakStatusEndring));
    }

    public void sendVedtakSendt(KafkaVedtakSendt vedtakSendt) {
        send(KafkaTopic.VEDTAK_SENDT, vedtakSendt.getAktorId(), toJson(vedtakSendt));
    }

    public void sendTidligereFeilet(FeiletKafkaMelding feiletKafkaMelding) {
        kafkaTemplate.send(kafkaTopicToStr(feiletKafkaMelding.getTopic()), feiletKafkaMelding.getKey(), feiletKafkaMelding.getJsonPayload())
                .addCallback(
                        sendResult -> onSuccessTidligereFeilet(feiletKafkaMelding),
                        throwable -> onErrorTidligereFeilet(feiletKafkaMelding, throwable)
                );
    }

    private void send(KafkaTopic kafkaTopic, String key, String jsonPayload) {
        String topic = kafkaTopicToStr(kafkaTopic);
        kafkaTemplate.send(topic, key, jsonPayload)
                .addCallback(
                        sendResult -> onSuccess(topic, key),
                        throwable -> onError(kafkaTopic, key, jsonPayload, throwable)
                );
    }

    private String kafkaTopicToStr(KafkaTopic topic) {
        switch (topic) {
            case VEDTAK_SENDT:
                return kafkaTopics.getVedtakSendt();
            case VEDTAK_STATUS_ENDRING:
                return kafkaTopics.getVedtakStatusEndring();
            default:
                throw new IllegalArgumentException("Unknown topic " + getName(topic));
        }
    }

    private void onSuccess(String topic, String key) {
        log.info(format("Publiserte melding på topic %s med key %s", topic, key));
    }

    private void onError(KafkaTopic topic, String key, String jsonPayload, Throwable throwable) {
        log.error(format("Kunne ikke publisere melding på topic %s med key %s \nERROR: %s", kafkaTopicToStr(topic), key, throwable));
        kafkaRepository.lagreFeiletKafkaMelding(topic, key, jsonPayload);
    }

    private void onSuccessTidligereFeilet(FeiletKafkaMelding feiletKafkaMelding) {
        String topic =  kafkaTopicToStr(feiletKafkaMelding.getTopic());
        String key = feiletKafkaMelding.getKey();

        log.info(format("Publiserte tidligere feilet melding på topic %s med key %s", topic, key));
        kafkaRepository.slettFeiletKafkaMelding(feiletKafkaMelding.getId());
    }

    private void onErrorTidligereFeilet(FeiletKafkaMelding feiletKafkaMelding, Throwable throwable) {
        KafkaTopic kafkaTopic = feiletKafkaMelding.getTopic();
        String topic = kafkaTopicToStr(kafkaTopic);
        String key = feiletKafkaMelding.getKey();

        log.error(format("Kunne ikke publisere tidligere feilet melding på topic %s med key %s \nERROR: %s", topic, key, throwable));
    }

}

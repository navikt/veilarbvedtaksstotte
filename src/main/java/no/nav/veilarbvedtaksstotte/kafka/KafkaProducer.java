package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.veilarbvedtaksstotte.repository.domain.FeiletKafkaMelding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static no.nav.common.json.JsonUtils.toJson;

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
        send(KafkaTopics.Topic.VEDTAK_STATUS_ENDRING, vedtakStatusEndring.getAktorId(), toJson(vedtakStatusEndring));
    }

    public void sendVedtakSendt(KafkaVedtakSendt vedtakSendt) {
        send(KafkaTopics.Topic.VEDTAK_SENDT, vedtakSendt.getAktorId(), toJson(vedtakSendt));
    }

    public void sendTidligereFeilet(FeiletKafkaMelding feiletKafkaMelding) {
        kafkaTemplate.send(kafkaTopics.topicToStr(feiletKafkaMelding.getTopic()), feiletKafkaMelding.getKey(), feiletKafkaMelding.getJsonPayload())
                .addCallback(
                        sendResult -> onSuccessTidligereFeilet(feiletKafkaMelding),
                        throwable -> onErrorTidligereFeilet(feiletKafkaMelding, throwable)
                );
    }

    private void send(KafkaTopics.Topic kafkaTopic, String key, String jsonPayload) {
        String topic = kafkaTopics.topicToStr(kafkaTopic);
        kafkaTemplate.send(topic, key, jsonPayload)
                .addCallback(
                        sendResult -> onSuccess(topic, key),
                        throwable -> onError(kafkaTopic, key, jsonPayload, throwable)
                );
    }

    private void onSuccess(String topic, String key) {
        log.info(format("Publiserte melding på topic %s med key %s", topic, key));
    }

    private void onError(KafkaTopics.Topic topic, String key, String jsonPayload, Throwable throwable) {
        log.error(format("Kunne ikke publisere melding på topic %s med key %s \nERROR: %s", kafkaTopics.topicToStr(topic), key, throwable));
        kafkaRepository.lagreFeiletProdusertKafkaMelding(topic, key, jsonPayload);
    }

    private void onSuccessTidligereFeilet(FeiletKafkaMelding feiletKafkaMelding) {
        String topic =  kafkaTopics.topicToStr(feiletKafkaMelding.getTopic());
        String key = feiletKafkaMelding.getKey();

        log.info(format("Publiserte tidligere feilet melding på topic %s med key %s", topic, key));
        kafkaRepository.slettFeiletKafkaMelding(feiletKafkaMelding.getId());
    }

    private void onErrorTidligereFeilet(FeiletKafkaMelding feiletKafkaMelding, Throwable throwable) {
        KafkaTopics.Topic kafkaTopic = feiletKafkaMelding.getTopic();
        String topic = kafkaTopics.topicToStr(kafkaTopic);
        String key = feiletKafkaMelding.getKey();

        log.error(format("Kunne ikke publisere tidligere feilet melding på topic %s med key %s \nERROR: %s", topic, key, throwable));
    }

}

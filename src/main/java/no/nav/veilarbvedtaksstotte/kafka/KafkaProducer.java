package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.FeiletKafkaMelding;
import no.nav.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.KafkaVedtakStatusEndring;
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

    private final KafkaProperties kafkaProperties;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final KafkaRepository kafkaRepository;

    @Autowired
    public KafkaProducer(KafkaProperties kafkaProperties, KafkaTemplate<String, String> kafkaTemplate, KafkaRepository kafkaRepository) {
        this.kafkaProperties = kafkaProperties;
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
                        throwable -> onError(topic, key, throwable)
                );
    }

    private String kafkaTopicToStr(KafkaTopic topic) {
        switch (topic) {
            case VEDTAK_SENDT:
                return kafkaProperties.getTopicVedtakSendt();
            case VEDTAK_STATUS_ENDRING:
                return kafkaProperties.getTopicVedtakStatusEndring();
            default:
                throw new IllegalArgumentException("Unknown topic " + getName(topic));
        }
    }

    private void onSuccess(String topic, String key) {
        log.info(format("Publiserte melding på topic %s med key %s", topic, key));
    }

    private void onError(String topic, String key, Throwable throwable) {
        log.error(format("Kunne ikke publisere melding på topic %s med key %s \nERROR: %s", topic, key, throwable));
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
        String jsonPayload = feiletKafkaMelding.getJsonPayload();

        log.error(format("Kunne ikke publisere tidligere feilet melding på topic %s med key %s \nERROR: %s", topic, key, throwable));
        kafkaRepository.lagreFeiletKafkaMelding(kafkaTopic, key, jsonPayload);
    }

}

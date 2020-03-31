package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.FeiletKafkaMelding;
import no.nav.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.enums.KafkaTopic;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import org.springframework.kafka.core.KafkaTemplate;

import javax.inject.Inject;

import static no.nav.json.JsonUtils.toJson;
import static no.nav.veilarbvedtaksstotte.config.KafkaProducerConfig.KAFKA_TOPIC_VEDTAK_SENDT;

@Slf4j
public class VedtakSendtTemplate {

    private final String topic;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaRepository kafkaRepository;

    @Inject
    public VedtakSendtTemplate(KafkaTemplate<String, String> kafkaTemplate, String topic, KafkaRepository kafkaRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.kafkaRepository = kafkaRepository;
    }

    public void send(KafkaVedtakSendt vedtakSendt) {
        final String serialisertData = toJson(vedtakSendt);
        kafkaTemplate.send(topic, vedtakSendt.getAktorId(), serialisertData)
                .addCallback(
                        sendResult -> {},
                        throwable -> onError(throwable, vedtakSendt.getAktorId(), serialisertData)
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
        log.error("Kunne ikke publisere tidligere feilet melding på topic: " + KAFKA_TOPIC_VEDTAK_SENDT + "\nERROR: " + throwable);
    }

    private void onError(Throwable throwable, String key, String jsonPayload) {
        log.error("Kunne ikke publisere tidligere feilet melding på topic: " + KAFKA_TOPIC_VEDTAK_SENDT + "\nERROR: " + throwable);
        kafkaRepository.lagreFeiletKafkaMelding(KafkaTopic.VEDTAK_SENDT, key, jsonPayload);
    }

}

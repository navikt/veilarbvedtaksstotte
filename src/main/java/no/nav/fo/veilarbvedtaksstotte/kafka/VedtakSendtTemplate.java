package no.nav.fo.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.fo.veilarbvedtaksstotte.repository.KafkaRepository;
import org.springframework.kafka.core.KafkaTemplate;

import javax.inject.Inject;

import static no.nav.fo.veilarbvedtaksstotte.config.KafkaConfig.KAFKA_TOPIC;
import static no.nav.json.JsonUtils.toJson;

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
       send(vedtakSendt, false);
    }

    public void sendTidligereFeilet(KafkaVedtakSendt vedtakSendt) {
        send(vedtakSendt, true);
    }

    private void send(KafkaVedtakSendt vedtakSendt, boolean harFeiletTidligere) {
        final String serialisertBruker = toJson(vedtakSendt);
        kafkaTemplate.send(topic, vedtakSendt.getAktorId(), serialisertBruker)
                .addCallback(
                        sendResult -> onSuccess(vedtakSendt, harFeiletTidligere),
                        throwable -> onError(throwable, vedtakSendt, harFeiletTidligere)
                );
    }

    private void onSuccess(KafkaVedtakSendt vedtakSendt, boolean harFeiletTidligere) {
        log.info("Publiserte melding for aktorId:" + vedtakSendt.getAktorId() + " på topic: " + KAFKA_TOPIC);

        if (harFeiletTidligere) {
            kafkaRepository.slettVedtakSendtKafkaFeil(vedtakSendt.getAktorId());
        }
    }

    private void onError(Throwable throwable, KafkaVedtakSendt vedtakSendt, boolean harFeiletTidligere) {
        log.error("Kunne ikke publisere melding for aktorId: " + vedtakSendt.getAktorId() +
                " på topic: " + KAFKA_TOPIC + "\nERROR: " + throwable);

        if (!harFeiletTidligere) {
            kafkaRepository.lagreVedtakSendtKafkaFeil(vedtakSendt);
        }
    }

}

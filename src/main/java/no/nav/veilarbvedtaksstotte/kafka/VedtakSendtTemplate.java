package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import org.springframework.kafka.core.KafkaTemplate;

import javax.inject.Inject;

import static no.nav.veilarbvedtaksstotte.config.KafkaProducerConfig.KAFKA_TOPIC_VEDTAK_SENDT;
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
        log.info("Publiserte melding for aktorId:" + vedtakSendt.getAktorId() + " på topic: " + KAFKA_TOPIC_VEDTAK_SENDT);

        if (harFeiletTidligere) {
            kafkaRepository.slettVedtakSendtKafkaFeil(vedtakSendt.getAktorId());
        }
    }

    private void onError(Throwable throwable, KafkaVedtakSendt vedtakSendt, boolean harFeiletTidligere) {
        log.error("Kunne ikke publisere melding for aktorId: " + vedtakSendt.getAktorId() +
                " på topic: " + KAFKA_TOPIC_VEDTAK_SENDT + "\nERROR: " + throwable);

        if (!harFeiletTidligere) {
            kafkaRepository.lagreVedtakSendtKafkaFeil(vedtakSendt);
        }
    }

}

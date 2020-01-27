package no.nav.fo.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakStatus;
import no.nav.fo.veilarbvedtaksstotte.repository.KafkaRepository;
import org.springframework.kafka.core.KafkaTemplate;

import javax.inject.Inject;

import static no.nav.fo.veilarbvedtaksstotte.config.KafkaProducerConfig.KAFKA_TOPIC_VEDTAK_SENDT;
import static no.nav.json.JsonUtils.toJson;

@Slf4j
public class VedtakStatusTemplate {

    private final String topic;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaRepository kafkaRepository;

    @Inject
    public VedtakStatusTemplate(KafkaTemplate<String, String> kafkaTemplate, String topic, KafkaRepository kafkaRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.kafkaRepository = kafkaRepository;
    }

    public void send(KafkaVedtakStatus kafkaVedtakStatus) {
        send(kafkaVedtakStatus, false);
    }

    public void sendTidligereFeilet(KafkaVedtakStatus kafkaVedtakStatus) {
        send(kafkaVedtakStatus, true);
    }

    private void send(KafkaVedtakStatus kafkaVedtakStatus, boolean harFeiletTidligere) {
        final String serialisertBruker = toJson(kafkaVedtakStatus);
        kafkaTemplate.send(topic, kafkaVedtakStatus.getBrukerAktorId(), serialisertBruker)
                .addCallback(
                        sendResult -> onSuccess(kafkaVedtakStatus, harFeiletTidligere),
                        throwable -> onError(throwable, kafkaVedtakStatus, harFeiletTidligere)
                );
    }

    private void onSuccess(KafkaVedtakStatus kafkaVedtakStatus, boolean harFeiletTidligere) {
        log.info("Publiserte melding for aktorId:" + kafkaVedtakStatus.getBrukerAktorId() + " på topic: " + KAFKA_TOPIC_VEDTAK_SENDT);

        if (harFeiletTidligere) {
            // kafkaRepository.slettVedtakSendtKafkaFeil(kafkaVedtakStatus.getBrukerAktorId());
        }
    }

    private void onError(Throwable throwable, KafkaVedtakStatus kafkaVedtakStatus, boolean harFeiletTidligere) {
        log.error("Kunne ikke publisere melding for aktorId: " + kafkaVedtakStatus.getBrukerAktorId() +
                " på topic: " + KAFKA_TOPIC_VEDTAK_SENDT + "\nERROR: " + throwable);

        if (!harFeiletTidligere) {
            // kafkaRepository.lagreVedtakSendtKafkaFeil(kafkaVedtakStatus);
        }
    }

}

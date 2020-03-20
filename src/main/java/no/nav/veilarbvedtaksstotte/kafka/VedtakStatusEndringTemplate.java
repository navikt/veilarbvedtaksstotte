package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.veilarbvedtaksstotte.config.KafkaProducerConfig;
import org.springframework.kafka.core.KafkaTemplate;

import javax.inject.Inject;

import static no.nav.json.JsonUtils.toJson;

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
        send(kafkaVedtakStatusEndring, false);
    }

    public void sendTidligereFeilet(KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        send(kafkaVedtakStatusEndring, true);
    }

    private void send(KafkaVedtakStatusEndring kafkaVedtakStatusEndring, boolean harFeiletTidligere) {
        final String serialisertBruker = toJson(kafkaVedtakStatusEndring);
        kafkaTemplate.send(topic, kafkaVedtakStatusEndring.getAktorId(), serialisertBruker)
                .addCallback(
                        sendResult -> onSuccess(kafkaVedtakStatusEndring, harFeiletTidligere),
                        throwable -> onError(throwable, kafkaVedtakStatusEndring, harFeiletTidligere)
                );
    }

    private void onSuccess(KafkaVedtakStatusEndring kafkaVedtakStatusEndring, boolean harFeiletTidligere) {
        log.info("Publiserte melding for aktorId:" + kafkaVedtakStatusEndring.getAktorId() + " på topic: " + KafkaProducerConfig.KAFKA_TOPIC_VEDTAK_STATUS_ENDRING);

        if (harFeiletTidligere) {
            kafkaRepository.slettVedtakStatusEndringKafkaFeil(
                    kafkaVedtakStatusEndring.getVedtakId(),
                    kafkaVedtakStatusEndring.getVedtakStatus()
            );
        }
    }

    private void onError(Throwable throwable, KafkaVedtakStatusEndring kafkaVedtakStatusEndring, boolean harFeiletTidligere) {
        log.error("Kunne ikke publisere melding for aktorId: " + kafkaVedtakStatusEndring.getAktorId() +
                " på topic: " + KafkaProducerConfig.KAFKA_TOPIC_VEDTAK_STATUS_ENDRING + "\nERROR: " + throwable);

        if (!harFeiletTidligere) {
            kafkaRepository.lagreVedtakStatusEndringKafkaFeil(kafkaVedtakStatusEndring);
        }
    }

}
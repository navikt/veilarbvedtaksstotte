package no.nav.fo.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaAvsluttOppfolging;
import no.nav.fo.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;

@Component
@Slf4j
public class AvsluttOpfolgingTemplate {

    public static final String ENDRING_PAA_AVSLUTTOPPFOLGING_KAFKA_TOPIC_PROPERTY_NAME = "ENDRING_PAA_AVSLUTTOPPFOLGING_TOPIC";

    private final VedtakService vedtakService;

    @Inject
    public AvsluttOpfolgingTemplate(VedtakService vedtakService, ConsumerParameters consumerParameters) {
        this.vedtakService = vedtakService;
        setProperty(ENDRING_PAA_AVSLUTTOPPFOLGING_KAFKA_TOPIC_PROPERTY_NAME, consumerParameters.topic, PUBLIC);
    }

    @KafkaListener(topics = "${" + ENDRING_PAA_AVSLUTTOPPFOLGING_KAFKA_TOPIC_PROPERTY_NAME + "}", groupId = "veilarbvedtaksstotte-consumer" )
    public void consume(@Payload String kafkaMelding) {
        try {
            KafkaAvsluttOppfolging melding = fromJson(kafkaMelding, KafkaAvsluttOppfolging.class);
            log.info("Leser melding :" + melding + " på topic: " + getOptionalProperty(ENDRING_PAA_AVSLUTTOPPFOLGING_KAFKA_TOPIC_PROPERTY_NAME));
            vedtakService.behandleAvsluttOppfolging(melding);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding: {}\n{}", kafkaMelding, t.getMessage(), t);
        }
    }

    public static class ConsumerParameters {
        public final String topic;

        public ConsumerParameters(String topic) {
            this.topic = topic;
        }
    }
}

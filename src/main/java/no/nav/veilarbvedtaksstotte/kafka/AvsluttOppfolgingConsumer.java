package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.config.KafkaConsumerConfig;
import no.nav.veilarbvedtaksstotte.domain.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.common.utils.EnvironmentUtils.Type.PUBLIC;
import static no.nav.common.utils.EnvironmentUtils.getOptionalProperty;
import static no.nav.common.utils.EnvironmentUtils.setProperty;

@Profile("!local")
@Component
@Slf4j
public class AvsluttOppfolgingConsumer {
    private static final String ENDRING_PAA_AVSLUTTOPPFOLGING_KAFKA_TOPIC_PROPERTY_NAME = "ENDRING_PAA_AVSLUTTOPPFOLGING_TOPIC";
    private final VedtakService vedtakService;

    @Autowired
    public AvsluttOppfolgingConsumer(VedtakService vedtakService, ConsumerParameters consumerParameters) {
        this.vedtakService = vedtakService;
        setProperty(ENDRING_PAA_AVSLUTTOPPFOLGING_KAFKA_TOPIC_PROPERTY_NAME, consumerParameters.topic, PUBLIC);
    }

    @KafkaListener(
            topics = "${" + ENDRING_PAA_AVSLUTTOPPFOLGING_KAFKA_TOPIC_PROPERTY_NAME + "}",
            containerFactory = KafkaConsumerConfig.AVSLUTT_OPPFOLGING_CONTAINER_FACTORY_NAME
    )
    public void consume(@Payload String kafkaMelding) {
        try {
            KafkaAvsluttOppfolging melding = fromJson(kafkaMelding, KafkaAvsluttOppfolging.class);
            log.info("Leser melding for aktorId:" + melding.getAktorId() + " på topic: " + getOptionalProperty(ENDRING_PAA_AVSLUTTOPPFOLGING_KAFKA_TOPIC_PROPERTY_NAME));
            vedtakService.behandleAvsluttOppfolging(melding);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding", t);
        }
    }

    public static class ConsumerParameters {
        public final String topic;

        public ConsumerParameters(String topic) {
            this.topic = topic;
        }
    }
}

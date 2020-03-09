package no.nav.fo.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaOppfolgingsbrukerEndring;
import no.nav.fo.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;

import static no.nav.fo.veilarbvedtaksstotte.config.KafkaConsumerConfig.OPPFOLGINGSBRUKER_ENDRING_CONTAINER_FACTORY_NAME;
import static no.nav.json.JsonUtils.fromJson;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.*;

@Slf4j
public class OppfolgingsbrukerEndringConsumer {

    private static final String KAFKA_CONSUMER_TOPIC = "aapen-fo-endringPaaOppfoelgingsBruker-v1-" + requireEnvironmentName();
    private static final String ENDRING_PAA_OPPFOLGINGSBRUKER_KAFKA_TOPIC_PROPERTY_NAME = "ENDRING_PAA_OPPFOLGINGSBRUKER_TOPIC";
    private final VedtakService vedtakService;

    public OppfolgingsbrukerEndringConsumer(VedtakService vedtakService) {
        this.vedtakService = vedtakService;
        setProperty(ENDRING_PAA_OPPFOLGINGSBRUKER_KAFKA_TOPIC_PROPERTY_NAME, KAFKA_CONSUMER_TOPIC, PUBLIC);
    }

    @KafkaListener(
            topics = "${" + ENDRING_PAA_OPPFOLGINGSBRUKER_KAFKA_TOPIC_PROPERTY_NAME + "}",
            containerFactory = OPPFOLGINGSBRUKER_ENDRING_CONTAINER_FACTORY_NAME
    )
    public void consume(@Payload String kafkaMelding) {
        try {
            KafkaOppfolgingsbrukerEndring melding = fromJson(kafkaMelding, KafkaOppfolgingsbrukerEndring.class);
            log.info("Leser melding for aktorId:" + melding.getOppfolgingsenhetId() + " på topic: " + getOptionalProperty(ENDRING_PAA_OPPFOLGINGSBRUKER_KAFKA_TOPIC_PROPERTY_NAME));
            vedtakService.behandleOppfolgingsbrukerEndring(melding);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding", t);
        }
    }
}

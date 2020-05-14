package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.config.KafkaConfig;
import no.nav.veilarbvedtaksstotte.domain.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.domain.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static no.nav.common.json.JsonUtils.fromJson;

@Slf4j
@Component
public class KafkaConsumer {

    private final KafkaProperties kafkaProperties;

    private final VedtakService vedtakService;

    @Autowired
    public KafkaConsumer(KafkaProperties kafkaProperties, VedtakService vedtakService) {
        this.kafkaProperties = kafkaProperties;
        this.vedtakService = vedtakService;
    }

    @KafkaListener(
            topics = "#{kafkaProperties.getTopicEndringPaAvsluttOppfolging()}",
            containerFactory = KafkaConfig.AVSLUTT_OPPFOLGING_CONTAINER_FACTORY_NAME
    )
    public void consumeEndringPaAvsluttOppfolging(@Payload String kafkaMelding) {
        try {
            KafkaAvsluttOppfolging melding = fromJson(kafkaMelding, KafkaAvsluttOppfolging.class);
            log.info("Leser melding for aktorId:" + melding.getAktorId() + " på topic: " + kafkaProperties.getTopicEndringPaAvsluttOppfolging());
            vedtakService.behandleAvsluttOppfolging(melding);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding fra topic " + kafkaProperties.getTopicEndringPaAvsluttOppfolging(), t);
        }
    }


    @KafkaListener(
            topics = "#{kafkaProperties.getTopicEndringPaOppfolgingBruker()}",
            containerFactory = KafkaConfig.OPPFOLGINGSBRUKER_ENDRING_CONTAINER_FACTORY_NAME
    )
    public void consumeEndringPaOppfolgingBruker(@Payload String kafkaMelding) {
        try {
            KafkaOppfolgingsbrukerEndring melding = fromJson(kafkaMelding, KafkaOppfolgingsbrukerEndring.class);
            log.info("Leser melding for aktorId:" + melding.getAktorId() + " på topic: " + kafkaProperties.getTopicEndringPaOppfolgingBruker());
            vedtakService.behandleOppfolgingsbrukerEndring(melding);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding fra topic " + kafkaProperties.getTopicEndringPaOppfolgingBruker(), t);
        }
    }

}

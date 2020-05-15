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

    private final KafkaTopicProperties kafkaTopicProperties;

    private final VedtakService vedtakService;

    @Autowired
    public KafkaConsumer(KafkaTopicProperties kafkaTopicProperties, VedtakService vedtakService) {
        System.out.println("PROPS======================================" + kafkaTopicProperties.getAllTopics().length);
        this.kafkaTopicProperties = kafkaTopicProperties;
        this.vedtakService = vedtakService;
    }

    @KafkaListener(
            topics = "#{kafkaTopicProperties.getEndringPaAvsluttOppfolging()}",
            containerFactory = KafkaConfig.AVSLUTT_OPPFOLGING_CONTAINER_FACTORY_NAME
    )
    public void consumeEndringPaAvsluttOppfolging(@Payload String kafkaMelding) {
        try {
            KafkaAvsluttOppfolging melding = fromJson(kafkaMelding, KafkaAvsluttOppfolging.class);
            log.info("Leser melding for aktorId:" + melding.getAktorId() + " på topic: " + kafkaTopicProperties.getEndringPaAvsluttOppfolging());
            vedtakService.behandleAvsluttOppfolging(melding);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding fra topic " + kafkaTopicProperties.getEndringPaAvsluttOppfolging(), t);
        }
    }


    @KafkaListener(
            topics = "#{kafkaTopicProperties.getEndringPaOppfolgingBruker()}",
            containerFactory = KafkaConfig.OPPFOLGINGSBRUKER_ENDRING_CONTAINER_FACTORY_NAME
    )
    public void consumeEndringPaOppfolgingBruker(@Payload String kafkaMelding) {
        try {
            KafkaOppfolgingsbrukerEndring melding = fromJson(kafkaMelding, KafkaOppfolgingsbrukerEndring.class);
            log.info("Leser melding for aktorId:" + melding.getAktorId() + " på topic: " + kafkaTopicProperties.getEndringPaOppfolgingBruker());
            vedtakService.behandleOppfolgingsbrukerEndring(melding);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding fra topic " + kafkaTopicProperties.getEndringPaOppfolgingBruker(), t);
        }
    }

}

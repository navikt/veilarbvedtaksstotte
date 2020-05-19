package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
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

    private final KafkaTopics kafkaTopics;

    private final VedtakService vedtakService;

    @Autowired
    public KafkaConsumer(KafkaTopics kafkaTopics, VedtakService vedtakService) {
        this.kafkaTopics = kafkaTopics;
        this.vedtakService = vedtakService;
    }

    @KafkaListener(topics = "#{kafkaTopics.getEndringPaAvsluttOppfolging()}")
    public void consumeEndringPaAvsluttOppfolging(@Payload String kafkaMelding) {
        try {
            KafkaAvsluttOppfolging melding = fromJson(kafkaMelding, KafkaAvsluttOppfolging.class);
            log.info("Leser melding for aktorId:" + melding.getAktorId() + " på topic: " + kafkaTopics.getEndringPaAvsluttOppfolging());
            vedtakService.behandleAvsluttOppfolging(melding);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding fra topic " + kafkaTopics.getEndringPaAvsluttOppfolging(), t);
        }
    }


    @KafkaListener(topics = "#{kafkaTopics.getEndringPaOppfolgingBruker()}")
    public void consumeEndringPaOppfolgingBruker(@Payload String kafkaMelding) {
        try {
            KafkaOppfolgingsbrukerEndring melding = fromJson(kafkaMelding, KafkaOppfolgingsbrukerEndring.class);
            log.info("Leser melding for aktorId:" + melding.getAktorId() + " på topic: " + kafkaTopics.getEndringPaOppfolgingBruker());
            vedtakService.behandleOppfolgingsbrukerEndring(melding);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding fra topic " + kafkaTopics.getEndringPaOppfolgingBruker(), t);
        }
    }

}

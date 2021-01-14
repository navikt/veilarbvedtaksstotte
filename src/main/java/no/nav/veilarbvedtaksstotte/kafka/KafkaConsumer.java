package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static no.nav.common.json.JsonUtils.fromJson;

@Slf4j
@Component
public class KafkaConsumer {

    private final int BACKOFF_TIME_MS = 60_000;

    private final KafkaTopics kafkaTopics;

    private final VedtakService vedtakService;

    private final KafkaRepository kafkaRepository;

    @Autowired
    public KafkaConsumer(KafkaTopics kafkaTopics, VedtakService vedtakService, KafkaRepository kafkaRepository) {
        this.kafkaTopics = kafkaTopics;
        this.vedtakService = vedtakService;
        this.kafkaRepository = kafkaRepository;
    }

    @KafkaListener(topics = "#{kafkaTopics.getEndringPaAvsluttOppfolging()}")
    public void consumeEndringPaAvsluttOppfolging(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        consumeWithErrorHandling(() -> {
            KafkaAvsluttOppfolging melding = fromJson(record.value(), KafkaAvsluttOppfolging.class);
            vedtakService.behandleAvsluttOppfolging(melding);
        }, record, acknowledgment);
    }

    @KafkaListener(topics = "#{kafkaTopics.getEndringPaOppfolgingBruker()}")
    public void consumeEndringPaOppfolgingBruker(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        consumeWithErrorHandling(() -> {
            KafkaOppfolgingsbrukerEndring melding = fromJson(record.value(), KafkaOppfolgingsbrukerEndring.class);
            vedtakService.behandleOppfolgingsbrukerEndring(melding);
        }, record, acknowledgment);
    }

    private void consumeWithErrorHandling(UnsafeRunnable runnable, ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String topic = record.topic();
        String key = record.key();
        String value = record.value();
        long offset = record.offset();

        log.info("topic={} key={} offset={} - Konsumerer melding fra topic", topic, key, offset);

        try {
            runnable.run();
            acknowledgment.acknowledge();
        } catch (Exception exception) {
            log.error(format("topic=%s key=%s offset=%d - Konsumering av melding feilet.", topic, key, offset), exception);

            try {
                // Map topic string to enum
                kafkaRepository.lagreFeiletKonsumertKafkaMelding(kafkaTopics.strToTopic(topic), key, value, offset);
                acknowledgment.acknowledge();
            } catch (Exception e) {
                log.error(format("topic=%s key=%s offset=%d - Lagring av feilet melding feilet", topic, key, offset), exception);
                acknowledgment.nack(BACKOFF_TIME_MS);
            }
        }
    }

}

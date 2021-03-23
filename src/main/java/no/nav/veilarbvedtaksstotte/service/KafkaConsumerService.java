package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaOppfolgingsbrukerEndring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
    private final VedtakService vedtakService;

    @Autowired
    public KafkaConsumerService(@Lazy VedtakService vedtakService) {
        this.vedtakService = vedtakService;
    }

    public void behandleEndringPaAvsluttOppfolging(KafkaAvsluttOppfolging kafkaAvsluttOppfolging) {
        vedtakService.behandleAvsluttOppfolging(kafkaAvsluttOppfolging);
    }

    public void behandleEndringPaOppfolgingsbruker(KafkaOppfolgingsbrukerEndring kafkaOppfolgingsbrukerEndring) {
        vedtakService.behandleOppfolgingsbrukerEndring(kafkaOppfolgingsbrukerEndring);
    }
}

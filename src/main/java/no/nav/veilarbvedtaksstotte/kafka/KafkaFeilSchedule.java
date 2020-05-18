package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.FeiletKafkaMelding;
import no.nav.veilarbvedtaksstotte.domain.enums.KafkaTopic;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class KafkaFeilSchedule {

    private final static long SCHEDULE_DELAY = 15 * 60 * 1000; // 15 minutes

    private final KafkaRepository kafkaRepository;

    private final KafkaProducer kafkaProducer;

    @Autowired
    public KafkaFeilSchedule(KafkaRepository kafkaRepository, KafkaProducer kafkaProducer) {
        this.kafkaRepository = kafkaRepository;
        this.kafkaProducer = kafkaProducer;
    }

    @Scheduled(fixedDelay = SCHEDULE_DELAY, initialDelay = 60 * 1000)
    public void sendVedtakSendtFeiledeKafkaMeldinger() {
        List<FeiletKafkaMelding> feiledeMeldinger = kafkaRepository.hentFeiledeKafkaMeldinger(KafkaTopic.VEDTAK_SENDT);
        feiledeMeldinger.forEach(kafkaProducer::sendTidligereFeilet);
    }

    @Scheduled(fixedDelay = SCHEDULE_DELAY, initialDelay = 60 * 1000)
    public void sendVedtakStatusFeiledeKafkaMeldinger() {
        List<FeiletKafkaMelding> feiledeMeldinger = kafkaRepository.hentFeiledeKafkaMeldinger(KafkaTopic.VEDTAK_STATUS_ENDRING);
        feiledeMeldinger.forEach(kafkaProducer::sendTidligereFeilet);
    }

}

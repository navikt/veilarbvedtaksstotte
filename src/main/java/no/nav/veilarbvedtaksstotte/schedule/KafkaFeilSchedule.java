package no.nav.veilarbvedtaksstotte.schedule;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.FeiletKafkaMelding;
import no.nav.veilarbvedtaksstotte.domain.enums.KafkaTopic;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.veilarbvedtaksstotte.service.KafkaService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@Component
public class KafkaFeilSchedule {

    public static final long SCHEDULE_DELAY = 15 * 60 * 1000; // 15 minutes

    private KafkaRepository kafkaRepository;

    private KafkaService kafkaService;

    @Inject
    public KafkaFeilSchedule(KafkaRepository kafkaRepository, KafkaService kafkaService) {
        this.kafkaRepository = kafkaRepository;
        this.kafkaService = kafkaService;
    }

    @Scheduled(fixedDelay = SCHEDULE_DELAY, initialDelay = 60 * 1000)
    public void sendVedtakSendtFeiledeKafkaMeldinger() {
        List<FeiletKafkaMelding> feiledeMeldinger = kafkaRepository.hentFeiledeKafkaMeldinger(KafkaTopic.VEDTAK_SENDT);
        feiledeMeldinger.forEach(feiletMelding -> kafkaService.sendTidligereFeilet(feiletMelding));
    }

    @Scheduled(fixedDelay = SCHEDULE_DELAY, initialDelay = 60 * 1000)
    public void sendVedtakStatusFeiledeKafkaMeldinger() {
        List<FeiletKafkaMelding> feiledeMeldinger = kafkaRepository.hentFeiledeKafkaMeldinger(KafkaTopic.VEDTAK_STATUS_ENDRING);
        feiledeMeldinger.forEach(feiletMelding -> kafkaService.sendTidligereFeilet(feiletMelding));
    }

}

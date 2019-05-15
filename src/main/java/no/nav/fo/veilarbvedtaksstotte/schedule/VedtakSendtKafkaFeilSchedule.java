package no.nav.fo.veilarbvedtaksstotte.schedule;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.fo.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.fo.veilarbvedtaksstotte.service.KafkaService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@Component
public class VedtakSendtKafkaFeilSchedule {

    public static final long SCHEDULE_DELAY = 15 * 60 * 1000; // 15 minutes

    private KafkaRepository kafkaRepository;

    private KafkaService kafkaService;

    @Inject
    public VedtakSendtKafkaFeilSchedule(KafkaRepository kafkaRepository, KafkaService kafkaService) {
        this.kafkaRepository = kafkaRepository;
        this.kafkaService = kafkaService;
    }

    @Scheduled(fixedDelay = SCHEDULE_DELAY, initialDelay = 60 * 1000)
    public void sendFeiledeKafkaMeldinger() {
        List<KafkaVedtakSendt> feiledeMeldinger = kafkaRepository.hentFeiledeVedtakSendt();
        feiledeMeldinger.forEach(feiletMelding -> {
            kafkaService.sendTidligereFeiletVedtak(feiletMelding);
        });
    }

}

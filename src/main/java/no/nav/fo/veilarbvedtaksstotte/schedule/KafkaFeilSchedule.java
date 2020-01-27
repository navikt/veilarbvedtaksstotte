package no.nav.fo.veilarbvedtaksstotte.schedule;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakStatus;
import no.nav.fo.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.fo.veilarbvedtaksstotte.service.KafkaService;
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
        List<KafkaVedtakSendt> feiledeMeldinger = kafkaRepository.hentFeiledeVedtakSendt();
        feiledeMeldinger.forEach(feiletMelding -> {
            kafkaService.sendTidligereFeiletVedtak(feiletMelding);
        });
    }

    @Scheduled(fixedDelay = SCHEDULE_DELAY, initialDelay = 60 * 1000)
    public void sendVedtakStatusFeiledeKafkaMeldinger() {
        List<KafkaVedtakStatus> feiledeMeldinger = null; // TODO
        feiledeMeldinger.forEach(feiletMelding -> {
            kafkaService.sendTidligereFeiletVedtakStatus(feiletMelding);
        });
    }

}

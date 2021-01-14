package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.veilarbvedtaksstotte.repository.domain.FeiletKafkaMelding;
import no.nav.veilarbvedtaksstotte.repository.domain.MeldingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class KafkaFeilSchedule {

    private final static long FIFTEEN_MINUTES = 15 * 60 * 1000;

    private final static long ONE_MINUTE = 60 * 1000;

    private final LeaderElectionClient leaderElectionClient;

    private final KafkaRepository kafkaRepository;

    private final KafkaProducer kafkaProducer;

    @Autowired
    public KafkaFeilSchedule(LeaderElectionClient leaderElectionClient, KafkaRepository kafkaRepository, KafkaProducer kafkaProducer) {
        this.leaderElectionClient = leaderElectionClient;
        this.kafkaRepository = kafkaRepository;
        this.kafkaProducer = kafkaProducer;
    }

    @Scheduled(fixedDelay = FIFTEEN_MINUTES, initialDelay = ONE_MINUTE)
    public void sendVedtakSendtFeiledeKafkaMeldinger() {
        if (leaderElectionClient.isLeader()) {
            List<FeiletKafkaMelding> feiledeMeldinger = kafkaRepository.hentFeiledeKafkaMeldinger(KafkaTopics.Topic.VEDTAK_SENDT, MeldingType.CONSUMED);
            feiledeMeldinger.forEach(kafkaProducer::sendTidligereFeilet);
        }
    }

    @Scheduled(fixedDelay = FIFTEEN_MINUTES, initialDelay = ONE_MINUTE)
    public void sendVedtakStatusFeiledeKafkaMeldinger() {
        if (leaderElectionClient.isLeader()) {
            List<FeiletKafkaMelding> feiledeMeldinger = kafkaRepository.hentFeiledeKafkaMeldinger(KafkaTopics.Topic.VEDTAK_STATUS_ENDRING, MeldingType.CONSUMED);
            feiledeMeldinger.forEach(kafkaProducer::sendTidligereFeilet);
        }
    }

}

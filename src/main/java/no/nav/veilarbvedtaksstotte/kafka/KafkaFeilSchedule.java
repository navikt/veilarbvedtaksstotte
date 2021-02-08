package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.veilarbvedtaksstotte.repository.domain.FeiletKafkaMelding;
import no.nav.veilarbvedtaksstotte.repository.domain.MeldingType;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.String.format;
import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;

@Slf4j
@Component
public class KafkaFeilSchedule {

    private final static long FIFTEEN_MINUTES = 15 * 60 * 1000;

    private final static long ONE_MINUTE = 60 * 1000;

    private final LeaderElectionClient leaderElectionClient;

    private final KafkaRepository kafkaRepository;

    private final KafkaProducer kafkaProducer;

    private final KafkaTopics kafkaTopics;

    private final VedtakService vedtakService;


    @Autowired
    public KafkaFeilSchedule(
            LeaderElectionClient leaderElectionClient,
            KafkaRepository kafkaRepository,
            KafkaProducer kafkaProducer,
            KafkaTopics kafkaTopics, VedtakService vedtakService
    ) {
        this.leaderElectionClient = leaderElectionClient;
        this.kafkaRepository = kafkaRepository;
        this.kafkaProducer = kafkaProducer;
        this.kafkaTopics = kafkaTopics;
        this.vedtakService = vedtakService;
    }

    @Scheduled(fixedDelay = FIFTEEN_MINUTES, initialDelay = ONE_MINUTE)
    public void publiserFeiletKafkaMeldinger() {
        if (leaderElectionClient.isLeader()) {
            List<FeiletKafkaMelding> feiledeMeldinger = kafkaRepository.hentFeiledeKafkaMeldinger(MeldingType.PRODUCED);
            feiledeMeldinger.forEach(kafkaProducer::sendTidligereFeilet);
        }
    }

    @Scheduled(fixedDelay = FIFTEEN_MINUTES, initialDelay = ONE_MINUTE)
    public void konsumerFeiletKafkaMeldinger() {
        if (leaderElectionClient.isLeader()) {
            List<FeiletKafkaMelding> feiledeMeldinger = kafkaRepository.hentFeiledeKafkaMeldinger(MeldingType.CONSUMED);
            feiledeMeldinger.forEach(this::konsumerFeiletKafkaMelding);
        }
    }

    private void konsumerFeiletKafkaMelding(FeiletKafkaMelding feiletKafkaMelding) {
        KafkaTopics.Topic topic = feiletKafkaMelding.getTopic();
        String json = feiletKafkaMelding.getJsonPayload();

        try {
            if (KafkaTopics.Topic.ENDRING_PA_AVSLUTT_OPPFOLGING.equals(topic)) {
                KafkaAvsluttOppfolging melding = fromJson(json, KafkaAvsluttOppfolging.class);
                vedtakService.behandleAvsluttOppfolging(melding);
            } else if (KafkaTopics.Topic.ENDRING_PA_OPPFOLGING_BRUKER.equals(topic)) {
                KafkaOppfolgingsbrukerEndring melding = fromJson(json, KafkaOppfolgingsbrukerEndring.class);
                vedtakService.behandleOppfolgingsbrukerEndring(melding);
            } else {
                throw new IllegalArgumentException("Det har ikke blitt implementert logikk for å håndtere republisering til " + getName(topic));
            }

            kafkaRepository.slettFeiletKafkaMelding(feiletKafkaMelding.getId());
        } catch (Exception e) {
            log.error(
                    format("topic=%s key=%s id=%d - Klarte ikke å konsumere feilet kafka melding",
                    kafkaTopics.topicToStr(topic), feiletKafkaMelding.getKey(), feiletKafkaMelding.getId()), e
            );
        }
    }

}

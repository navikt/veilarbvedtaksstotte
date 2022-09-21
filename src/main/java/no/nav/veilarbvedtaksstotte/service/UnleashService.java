package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.featuretoggle.UnleashClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UnleashService {

    private static final String VEILARBVEDTAKSSTOTTE_DOK_DIST_SCHEDULE_ENABLED_TOGGLE = "veilarbvedtaksstotte.dok_dist_schedule_enabled";
    private static final String KAFKA_KONSUMERING_SKRUDD_AV = "veilarbvedtaksstotte.kafka-konsumering-skrudd-av";

    private final UnleashClient unleashClient;

    @Autowired
    public UnleashService(UnleashClient unleashClient) {
        this.unleashClient = unleashClient;
    }

    public boolean isDokDistScheduleEnabled() {
        return unleashClient.isEnabled(VEILARBVEDTAKSSTOTTE_DOK_DIST_SCHEDULE_ENABLED_TOGGLE);
    }

    public boolean isKafkaKonsumeringSkruddAv() {
        return unleashClient.isEnabled(KAFKA_KONSUMERING_SKRUDD_AV);
    }
}

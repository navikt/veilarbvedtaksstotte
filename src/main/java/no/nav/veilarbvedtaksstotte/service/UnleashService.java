package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.featuretoggle.UnleashClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UnleashService {

    private final static String VEILARBVEDTAKSSTOTTE_NY_DOK_INTEGRASJON_ENABLED_TOGGLE = "veilarbvedtaksstotte.ny.dok.integrasjon.enabled";
    private final static String PDL_AKTOR_OPPSLAG = "veilarbvedtaksstotte.pdl-aktoroppslag";
    private final static String KAFKA_KONSUMERING_SKRUDD_AV = "veilarbvedtaksstotte.kafka-konsumering-skrudd-av";

    private final UnleashClient unleashClient;

    @Autowired
    public UnleashService(UnleashClient unleashClient) {
        this.unleashClient = unleashClient;
    }

    public boolean isNyDokIntegrasjonEnabled() {
        return unleashClient.isEnabled(VEILARBVEDTAKSSTOTTE_NY_DOK_INTEGRASJON_ENABLED_TOGGLE);
    }

    public boolean isPdlAktorOppslagEnabled() {
        return unleashClient.isEnabled(PDL_AKTOR_OPPSLAG);
    }

    public boolean isKafkaKonsumeringSkruddAv() {
        return unleashClient.isEnabled(KAFKA_KONSUMERING_SKRUDD_AV);
    }
}

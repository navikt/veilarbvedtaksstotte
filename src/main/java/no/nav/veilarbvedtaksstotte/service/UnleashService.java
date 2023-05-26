package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.featuretoggle.UnleashClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UnleashService {

    private static final String POAO_TILGANG_ENABLED = "veilarbvedtaksstotte.poao-tilgang-enabled";

    private final UnleashClient unleashClient;

    @Autowired
    public UnleashService(UnleashClient unleashClient) {
        this.unleashClient = unleashClient;
    }

    public boolean isPoaoTilgangEnabled() {
        return unleashClient.isEnabled(POAO_TILGANG_ENABLED);
    }

}

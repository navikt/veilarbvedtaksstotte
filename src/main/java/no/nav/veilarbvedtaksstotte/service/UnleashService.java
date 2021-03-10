package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.featuretoggle.UnleashClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UnleashService {

    private final static String VEILARBVEDTAKSSTOTTE_ENABLED_TOGGLE = "veilarbvedtaksstotte.enabled";
    private final static String PTO_VEDTAKSSTOTTE_PILOT_TOGGLE = "pto.vedtaksstotte.pilot";
    private final static String VEILARBVEDTAKSSTOTTE_NY_DOK_INTEGRASJON_ENABLED_TOGGLE = "veilarbvedtaksstotte.ny.dok.integrasjon.enabled";
    private final static String PDL_AKTOR_OPPSLAG = "veilarbvedtaksstotte.pdl-aktoroppslag";
    private final static String PDL_IDENT_OPPSLAG_DISABLED = "veilarbvedtaksstotte.pdl_identoppslag_disabled";

    private final UnleashClient unleashClient;

    @Autowired
    public UnleashService(UnleashClient unleashClient) {
        this.unleashClient = unleashClient;
    }

    public boolean isVedtaksstotteEnabled() {
        return unleashClient.isEnabled(VEILARBVEDTAKSSTOTTE_ENABLED_TOGGLE);
    }

    public boolean isVedtaksstottePilotEnabled() {
        return unleashClient.isEnabled(PTO_VEDTAKSSTOTTE_PILOT_TOGGLE);
    }

    public boolean isNyDokIntegrasjonEnabled() {
        return unleashClient.isEnabled(VEILARBVEDTAKSSTOTTE_NY_DOK_INTEGRASJON_ENABLED_TOGGLE);
    }

    public boolean isPdlAktorOppslagEnabled() {
        return unleashClient.isEnabled(PDL_AKTOR_OPPSLAG);
    }

    public boolean isPdlIdentOppslagDisabled() {
        return unleashClient.isEnabled(PDL_IDENT_OPPSLAG_DISABLED);
    }
}

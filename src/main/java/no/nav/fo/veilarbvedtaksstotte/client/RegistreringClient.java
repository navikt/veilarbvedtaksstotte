package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class RegistreringClient extends BaseClient {

    public static final String REGISTRERING_API_PROPERTY_NAME = "REGISTRERING_URL";
    public static final String VEILARBREGISTRERING = "veilarbregistrering";

    @Inject
    public RegistreringClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        super(getRequiredProperty(REGISTRERING_API_PROPERTY_NAME), httpServletRequestProvider);
    }

    public String hentRegistrering(String fnr) {
        return get(joinPaths(baseUrl, "api", "registrering?fnr=", fnr), String.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot veilarbregistrering/api/registrering?fnr="));
    }
}

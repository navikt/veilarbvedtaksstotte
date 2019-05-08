package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class EgenvurderingClient extends BaseClient {

    public static final String EGENVURDERING_API_PROPERTY_NAME = "EGENVURDERING_URL";
    public static final String VEILARBVEDTAKINFO = "veilarbvedtakinfo";

    @Inject
    public EgenvurderingClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        super(getRequiredProperty(EGENVURDERING_API_PROPERTY_NAME), httpServletRequestProvider);
    }

    public String hentEgenvurdering(String fnr) {
        return get(joinPaths(baseUrl, "api", "behovsvurdering", "besvarelse?fnr=", fnr), String.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot veilarbvedtakinfo/api/behovsvurdering/besvarelse?fnr="));
    }
}

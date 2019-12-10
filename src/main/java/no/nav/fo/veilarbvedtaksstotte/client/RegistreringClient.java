package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.RegistreringData;
import org.springframework.cache.annotation.Cacheable;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.fo.veilarbvedtaksstotte.config.CacheConfig.REGISTRERING_CACHE_NAME;
import static no.nav.json.JsonUtils.fromJson;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class RegistreringClient extends BaseClient {

    public static final String REGISTRERING_API_PROPERTY_NAME = "REGISTRERING_URL";
    public static final String VEILARBREGISTRERING = "veilarbregistrering";

    @Inject
    public RegistreringClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        super(getRequiredProperty(REGISTRERING_API_PROPERTY_NAME), httpServletRequestProvider);
    }

    @Cacheable(REGISTRERING_CACHE_NAME)
    public String hentRegistreringDataJson(String fnr) {
        String hentRegistreringUrl = joinPaths(baseUrl, "api", "registrering?fnr=") + fnr;
        RestResponse<String> response = get(hentRegistreringUrl, String.class);

        if (response.hasStatus(404) || response.hasStatus(204)) {
            return null;
        }

        if (response.getStatus() >= 400) {
            throw new RuntimeException("Feil ved kall mot " + hentRegistreringUrl);
        }

        return response.getData().orElseThrow(() -> new RuntimeException("Feil ved kall mot " + hentRegistreringUrl));
    }

    public RegistreringData hentRegistreringData(String fnr) {
        return fromJson(hentRegistreringDataJson(fnr), RegistreringData.class);
    }

}

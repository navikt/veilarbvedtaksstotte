package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestClient;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarbvedtaksstotte.domain.RegistreringData;
import no.nav.veilarbvedtaksstotte.utils.RestClientUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

@Slf4j
@Component
public class RegistreringClient {

    public static final String REGISTRERING_API_PROPERTY_NAME = "REGISTRERING_URL";
    public static final String VEILARBREGISTRERING = "veilarbregistrering";

    private final String veilarbregistreringUrl;

    public RegistreringClient() {
        this.veilarbregistreringUrl = EnvironmentUtils.getRequiredProperty(REGISTRERING_API_PROPERTY_NAME);
    }

//    @Cacheable(CacheConfig.REGISTRERING_CACHE_NAME)
    @SneakyThrows
    public String hentRegistreringDataJson(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbregistreringUrl, "/api/registrering?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            if (response.code() == 404 || response.code() == 204) {
                return null;
            }

            RestClientUtils.throwIfNotSuccessful(response);
            return response.body().string();
        }
    }

    public RegistreringData hentRegistreringData(String fnr) {
        String registreringData = hentRegistreringDataJson(fnr);
        return registreringData != null
                ? fromJson(registreringData, RegistreringData.class)
                : null;
    }

}

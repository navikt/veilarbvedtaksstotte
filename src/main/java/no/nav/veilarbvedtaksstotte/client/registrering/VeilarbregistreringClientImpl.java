package no.nav.veilarbvedtaksstotte.client.registrering;

import lombok.SneakyThrows;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.config.CacheConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class VeilarbregistreringClientImpl implements VeilarbregistreringClient {

    // Bruker veilarbperson som proxy til veilarbregistrering, som kaller videre med AAD token
    private final String veilarbpersonUrl;

    private final OkHttpClient client;

    private final AuthContextHolder authContextHolder;

    public VeilarbregistreringClientImpl(String veilarbpersonUrl, AuthContextHolder authContextHolder) {
        this.veilarbpersonUrl = veilarbpersonUrl;
        this.client = RestClient.baseClient();
        this.authContextHolder = authContextHolder;
    }

    @Cacheable(CacheConfig.REGISTRERING_CACHE_NAME)
    @SneakyThrows
    public String hentRegistreringDataJson(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbpersonUrl, "/api/person/registrering?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker(authContextHolder))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            if (response.code() == 404 || response.code() == 204) {
                return null;
            }

            RestUtils.throwIfNotSuccessful(response);
            return response.body().string();
        }
    }

    public RegistreringData hentRegistreringData(String fnr) {
        String registreringData = hentRegistreringDataJson(fnr);
        return registreringData != null
                ? fromJson(registreringData, RegistreringData.class)
                : null;
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbpersonUrl, "/internal/isAlive"), client);
    }

}

package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.config.CacheConfig;
import no.nav.veilarbvedtaksstotte.domain.RegistreringData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class RegistreringClient implements HealthCheck {

    private final String veilarbregistreringUrl;

    private final OkHttpClient client;

    public RegistreringClient(String veilarbregistreringUrl) {
        this.veilarbregistreringUrl = veilarbregistreringUrl;
        this.client = RestClient.baseClient();
    }

    @Cacheable(CacheConfig.REGISTRERING_CACHE_NAME)
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
        return HealthCheckUtils.pingUrl(joinPaths(veilarbregistreringUrl, "/internal/isReady"), client);
    }

}

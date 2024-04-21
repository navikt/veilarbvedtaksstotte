package no.nav.veilarbvedtaksstotte.client.registrering;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.registrering.dto.RegistreringResponseDto;
import no.nav.veilarbvedtaksstotte.client.registrering.request.RegistreringRequest;
import no.nav.veilarbvedtaksstotte.config.CacheConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;

import java.util.function.Supplier;

import static no.nav.common.rest.client.RestUtils.toJsonRequestBody;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.JsonUtils.fromJson;

public class VeilarbregistreringClientImpl implements VeilarbregistreringClient {

    // Bruker veilarbperson som proxy til veilarbregistrering, som kaller videre med AAD token
    private final String veilarbpersonUrl;

    private final OkHttpClient client;

    private final Supplier<String> userTokenSupplier;

    public VeilarbregistreringClientImpl(String veilarbpersonUrl, Supplier<String> userTokenSupplier) {
        this.veilarbpersonUrl = veilarbpersonUrl;
        this.client = RestClient.baseClient();
        this.userTokenSupplier = userTokenSupplier;
    }

    @Cacheable(CacheConfig.REGISTRERING_CACHE_NAME)
    @SneakyThrows
    public RegistreringResponseDto hentRegistreringData(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbpersonUrl, "/api/v3/person/hent-registrering"))
                .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
                .post(toJsonRequestBody(new RegistreringRequest(Fnr.of(fnr))))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            if (response.code() == 404 || response.code() == 204) {
                return null;
            }

            RestUtils.throwIfNotSuccessful(response);
            ResponseBody registreringDataJson = response.body();
            return registreringDataJson != null
                    ? fromJson(registreringDataJson.string(), RegistreringResponseDto.class)
                    : null;
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbpersonUrl, "/internal/isAlive"), client);
    }

}

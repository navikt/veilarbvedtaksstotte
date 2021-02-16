package no.nav.veilarbvedtaksstotte.client.veilarboppfolging;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.config.CacheConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.function.Supplier;

import static no.nav.common.utils.AuthUtils.bearerToken;
import static no.nav.common.utils.UrlUtils.joinPaths;

public class VeilarboppfolgingClientImpl implements VeilarboppfolgingClient {

    private final String veilarboppfolgingUrl;

    private final OkHttpClient client;

    private final Supplier<String> userTokenSupplier;

    private final Supplier<String> systemTokenSupplier;

    public VeilarboppfolgingClientImpl(String veilarboppfolgingUrl, Supplier<String> userTokenSupplier, Supplier<String> systemTokenSupplier) {
        this.veilarboppfolgingUrl = veilarboppfolgingUrl;
        this.client = RestClient.baseClient();
        this.userTokenSupplier = userTokenSupplier;
        this.systemTokenSupplier = systemTokenSupplier;
    }

    @Cacheable(CacheConfig.OPPFOLGING_CACHE_NAME)
    @SneakyThrows
    public OppfolgingDTO hentOppfolgingData(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/oppfolging?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(userTokenSupplier.get()))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.getBodyStr(response)
                    .map((bodyStr) -> JsonUtils.fromJson(bodyStr, OppfolgingDTO.class))
                    .orElseThrow(() -> new IllegalStateException("Unable to parse json"));
        }
    }

    @Cacheable(CacheConfig.OPPFOLGINGPERIODE_CACHE_NAME)
    @SneakyThrows
    public List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/oppfolging/oppfolgingsperioder?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(systemTokenSupplier.get()))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.getBodyStr(response)
                    .map((bodyStr) -> JsonUtils.fromJsonArray(bodyStr, OppfolgingPeriodeDTO.class))
                    .orElseThrow(() -> new IllegalStateException("Unable to parse json"));
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarboppfolgingUrl, "/internal/isReady"), client);
    }

}

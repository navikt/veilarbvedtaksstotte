package no.nav.veilarbvedtaksstotte.client.veilarboppfolging;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingStatusDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.SakDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.request.OppfolgingRequest;
import no.nav.veilarbvedtaksstotte.config.CacheConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static no.nav.common.rest.client.RestUtils.toJsonRequestBody;
import static no.nav.common.utils.AuthUtils.bearerToken;
import static no.nav.common.utils.UrlUtils.joinPaths;

public class VeilarboppfolgingClientImpl implements VeilarboppfolgingClient {

    private final String veilarboppfolgingUrl;

    private final OkHttpClient client;

    private final Supplier<String> machineToMachineTokenSupplier;

    public VeilarboppfolgingClientImpl(String veilarboppfolgingUrl, Supplier<String> machineToMachineTokenSupplier) {
        this.veilarboppfolgingUrl = veilarboppfolgingUrl;
        this.client = RestClient.baseClient();
        this.machineToMachineTokenSupplier = machineToMachineTokenSupplier;
    }
    @SneakyThrows
    public Optional<OppfolgingStatusDTO> erUnderOppfolging(Fnr fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/v3/hent-oppfolging"))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(machineToMachineTokenSupplier.get()))
                .post(toJsonRequestBody(new OppfolgingRequest(fnr)))
                .build();
        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.getBodyStr(response)
                    .map((bodyStr) -> JsonUtils.fromJson(bodyStr, OppfolgingStatusDTO.class));
        }
    }

    @Cacheable(CacheConfig.GJELDENDE_OPPFOLGINGPERIODE_CACHE_NAME)
    @SneakyThrows
    public Optional<OppfolgingPeriodeDTO> hentGjeldendeOppfolgingsperiode(Fnr fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/v3/oppfolging/hent-gjeldende-periode"))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(machineToMachineTokenSupplier.get()))
                .post(toJsonRequestBody(new OppfolgingRequest(fnr)))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.getBodyStr(response)
                    .map((bodyStr) -> JsonUtils.fromJson(bodyStr, OppfolgingPeriodeDTO.class));
        }
    }

    @Cacheable(CacheConfig.OPPFOLGINGPERIODE_SAK_CACHE_NAME)
    @SneakyThrows
    public SakDTO hentOppfolgingsperiodeSak(UUID oppfolgingsperiodeId) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/v3/sak/" + oppfolgingsperiodeId))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(machineToMachineTokenSupplier.get()))
                .post(RequestBody.create("", null))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, SakDTO.class);
        }
    }

    @Cacheable(CacheConfig.OPPFOLGINGPERIODER_CACHE_NAME)
    @SneakyThrows
    public List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(Fnr fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/v3/oppfolging/hent-perioder"))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(machineToMachineTokenSupplier.get()))
                .post(toJsonRequestBody(new OppfolgingRequest(fnr)))
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
        return HealthCheckUtils.pingUrl(joinPaths(veilarboppfolgingUrl, "/api/ping"), client);
    }

}

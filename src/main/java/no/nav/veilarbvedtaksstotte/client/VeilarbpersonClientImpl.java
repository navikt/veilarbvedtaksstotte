package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.client.api.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.domain.PersonNavn;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import no.nav.veilarbvedtaksstotte.utils.RestClientUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;

import java.util.function.Supplier;

import static no.nav.common.utils.UrlUtils.joinPaths;

public class VeilarbpersonClientImpl implements VeilarbpersonClient {

    private final String veilarbpersonUrl;

    private final OkHttpClient client;

    private final Supplier<String> userTokenSupplier;

    public VeilarbpersonClientImpl(String veilarbpersonUrl, Supplier<String> userTokenSupplier) {
        this.veilarbpersonUrl = veilarbpersonUrl;
        this.userTokenSupplier = userTokenSupplier;
        this.client = RestClient.baseClient();
    }

    @SneakyThrows
    public PersonNavn hentPersonNavn(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbpersonUrl, "/api/person/navn?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, RestClientUtils.bearerToken(userTokenSupplier.get()))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, PersonNavn.class);
        }
    }

    @SneakyThrows
    public String hentCVOgJobbprofil(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbpersonUrl, "/api/person/cv_jobbprofil?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, RestClientUtils.bearerToken(userTokenSupplier.get()))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {

            if (response.code() == 403 || response.code() == 401) {
                return JsonUtils.createNoDataStr("Bruker har ikke delt CV/jobbprofil med NAV");
            }

            if (response.code() == 204 || response.code() == 404) {
                return JsonUtils.createNoDataStr("Bruker har ikke fylt ut CV/jobbprofil");
            }

            return response.body().string();
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbpersonUrl, "/internal/isAlive"), client);
    }

}

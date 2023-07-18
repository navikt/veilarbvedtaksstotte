package no.nav.veilarbvedtaksstotte.client.egenvurdering;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;

import java.util.function.Supplier;

import static no.nav.common.utils.UrlUtils.joinPaths;

public class VeilarbvedtakinfoClientImpl implements VeilarbvedtakinfoClient {

    private final String veilarbvedtakInfoUrl;

    private final OkHttpClient client;

    private final Supplier<String>  userTokenSupplier;

    public VeilarbvedtakinfoClientImpl(String veilarbvedtakInfoUrl, Supplier<String> userTokenSupplier) {
        this.veilarbvedtakInfoUrl = veilarbvedtakInfoUrl;
        this.client = RestClient.baseClient();
        this.userTokenSupplier = userTokenSupplier;
    }

    @SneakyThrows
    public String hentEgenvurdering(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbvedtakInfoUrl, "/api/behovsvurdering/besvarelse?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);

            if (response.code() == 204) {
                return JsonUtils.createNoDataStr("Bruker har ikke fylt ut egenvurdering");
            }

            return response.body().string();
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbvedtakInfoUrl, "/internal/health/readiness"), client);
    }

}

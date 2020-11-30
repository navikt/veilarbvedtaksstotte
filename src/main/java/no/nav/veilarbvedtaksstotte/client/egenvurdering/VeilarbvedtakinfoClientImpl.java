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

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class VeilarbvedtakinfoClientImpl implements VeilarbvedtakinfoClient {

    private final String veilarbvedtakInfoUrl;

    private final OkHttpClient client;

    public VeilarbvedtakinfoClientImpl(String veilarbvedtakInfoUrl) {
        this.veilarbvedtakInfoUrl = veilarbvedtakInfoUrl;
        this.client = RestClient.baseClient();
    }

    @SneakyThrows
    public String hentEgenvurdering(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbvedtakInfoUrl, "/api/behovsvurdering/besvarelse?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
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

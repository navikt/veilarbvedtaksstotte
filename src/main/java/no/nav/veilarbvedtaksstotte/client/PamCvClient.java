package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class PamCvClient implements HealthCheck {

    private final String pamCvUrl;

    private final OkHttpClient client;

    public PamCvClient(String pamCvUrl) {
        this.pamCvUrl = pamCvUrl;
        this.client = RestClient.baseClient();
    }

    @SneakyThrows
    public String hentCV(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(pamCvUrl, "/rest/v1/arbeidssoker/", fnr))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
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
        return HealthCheckUtils.pingUrl(joinPaths(pamCvUrl, "/rest/internal/isReady"), client);
    }

}

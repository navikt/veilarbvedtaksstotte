package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.config.CacheConfig;
import no.nav.veilarbvedtaksstotte.domain.OppfolgingDTO;
import no.nav.veilarbvedtaksstotte.domain.OppfolgingstatusDTO;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class OppfolgingClient implements HealthCheck {

    private final String veilarboppfolgingUrl;

    private final OkHttpClient client;

    public OppfolgingClient(String veilarboppfolgingUrl) {
        this.veilarboppfolgingUrl = veilarboppfolgingUrl;
        this.client = RestClient.baseClient();
    }

    @SneakyThrows
    public String hentServicegruppe(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/person/", fnr, "/oppfolgingsstatus"))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseBodyOrThrow(response.body(), OppfolgingstatusDTO.class).getServicegruppe();
        }
    }

    @Cacheable(CacheConfig.OPPFOLGING_CACHE_NAME)
    @SneakyThrows
    public OppfolgingDTO hentOppfolgingData(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/oppfolging?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseBodyOrThrow(response.body(), OppfolgingDTO.class);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarboppfolgingUrl, "/internal/isReady"), client);
    }

}

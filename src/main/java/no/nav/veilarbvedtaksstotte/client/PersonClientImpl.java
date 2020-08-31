package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.client.api.PersonClient;
import no.nav.veilarbvedtaksstotte.domain.PersonNavn;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class PersonClientImpl implements PersonClient {

    private final String veilarbpersonUrl;

    private final OkHttpClient client;

    public PersonClientImpl(String veilarbpersonUrl) {
        this.veilarbpersonUrl = veilarbpersonUrl;
        this.client = RestClient.baseClient();
    }

    @SneakyThrows
    public PersonNavn hentPersonNavn(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbpersonUrl, "/api/person/navn?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, PersonNavn.class);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbpersonUrl, "/internal/isReady"), client);
    }

}

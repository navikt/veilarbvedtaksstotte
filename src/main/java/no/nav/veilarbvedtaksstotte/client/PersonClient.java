package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.domain.PersonNavn;
import no.nav.veilarbvedtaksstotte.utils.RestClientUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class PersonClient {

    private final String veilarbpersonUrl;

    public PersonClient(String veilarbpersonUrl) {
        this.veilarbpersonUrl = veilarbpersonUrl;
    }

    @SneakyThrows
    public PersonNavn hentPersonNavn(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbpersonUrl, "/api/person/navn?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestClientUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseBodyOrThrow(response.body(), PersonNavn.class);
        }
    }

}

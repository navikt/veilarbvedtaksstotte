package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarbvedtaksstotte.domain.PersonNavn;
import no.nav.veilarbvedtaksstotte.utils.RestClientUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

@Component
public class PersonClient {

    public static final String VEILARBPERSON_API_PROPERTY_NAME = "VEILARPERSONAPI_URL";
    public static final String VEILARBPERSON = "veilarbperson";

    private final String veilarbpersonUrl;

    public PersonClient() {
        this.veilarbpersonUrl = EnvironmentUtils.getRequiredProperty(VEILARBPERSON_API_PROPERTY_NAME);
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

package no.nav.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.domain.Oppfolgingsenhet;
import no.nav.veilarbvedtaksstotte.utils.RestClientUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

@Slf4j
@Component
public class ArenaClient {

    public static final String VEILARBARENA_API_PROPERTY_NAME = "VEILARBARENA_URL";
    public static final String VEILARBARENA = "veilarbarena";

    private final String veilarbarenaUrl;

    public ArenaClient() {
        this.veilarbarenaUrl = getRequiredProperty(VEILARBARENA_API_PROPERTY_NAME);
    }

    public String oppfolgingsenhet(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "/api/oppfolgingsbruker/", fnr))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestClientUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseBodyOrThrow(response.body(), Oppfolgingsenhet.class).getNavKontor();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot veilarbarena/oppfolgingsbruker");
        }
    }

}

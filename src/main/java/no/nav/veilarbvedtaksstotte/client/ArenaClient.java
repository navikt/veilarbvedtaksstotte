package no.nav.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.domain.Oppfolgingsenhet;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class ArenaClient {

    public static final String VEILARBARENA_API_PROPERTY_NAME = "VEILARBARENA_URL";
    public static final String VEILARBARENA = "veilarbarena";

    private final String veilarbarenaUrl;

    public ArenaClient() {
        this.veilarbarenaUrl = getRequiredProperty(VEILARBARENA_API_PROPERTY_NAME);
    }

    public String oppfolgingsenhet(String fnr) {
        Request request = new Request.Builder()
                .url(String.join("/", veilarbarenaUrl, "api", "oppfolgingsbruker", fnr))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {

            return RestUtils.parseJsonResponseBodyOrThrow(response.body(), Oppfolgingsenhet.class).getNavKontor();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot veilarbarena/oppfolgingsbruker");
        }

        return get(joinPaths(baseUrl, "api", "oppfolgingsbruker", fnr), Oppfolgingsenhet.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot veilarbarena/oppfolgingsbruker"))
                .getNavKontor();
    }

}

package no.nav.veilarbvedtaksstotte.client;

import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.domain.Oppfolgingsenhet;
import no.nav.veilarbvedtaksstotte.utils.RestClientUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class ArenaClient {

    private final String veilarbarenaUrl;

    public ArenaClient(String veilarbarenaUrl) {
        this.veilarbarenaUrl = veilarbarenaUrl;
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

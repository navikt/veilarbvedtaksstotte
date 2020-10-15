package no.nav.veilarbvedtaksstotte.utils;

import no.nav.common.auth.context.AuthContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RestClientUtils {

    public static String authHeaderMedInnloggetBruker() {
        String token = AuthContextHolder
                .getIdTokenString()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Fant ikke token til innlogget bruker"));

        return bearerToken(token);
    }

    public static String bearerToken(String token) {
        return "Bearer " + token;
    }

}

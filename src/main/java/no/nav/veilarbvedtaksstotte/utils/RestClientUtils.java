package no.nav.veilarbvedtaksstotte.utils;

import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.SubjectHandler;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RestClientUtils {

    public static String authHeaderMedInnloggetBruker() {
        String token = SubjectHandler.
                getSsoToken()
                .map(SsoToken::getToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Fant ikke token til innlogget bruker"));

        return bearerToken(token);
    }

    public static String bearerToken(String token) {
        return "Bearer " + token;
    }

}

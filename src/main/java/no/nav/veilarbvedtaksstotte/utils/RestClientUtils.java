package no.nav.veilarbvedtaksstotte.utils;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.common.utils.AuthUtils.bearerToken;

public class RestClientUtils {

    public static String authHeaderMedInnloggetBruker(AuthContextHolder authContextHolder) {
        String token = authContextHolder
                .getIdTokenString()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Fant ikke token til innlogget bruker"));

        return bearerToken(token);
    }
}

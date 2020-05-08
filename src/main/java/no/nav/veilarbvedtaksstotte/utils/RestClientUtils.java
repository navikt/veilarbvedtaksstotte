package no.nav.veilarbvedtaksstotte.utils;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.SubjectHandler;
import okhttp3.Response;

@Slf4j
public class RestClientUtils {

    public static void throwIfNotSuccessful(Response response) {
        if (!response.isSuccessful()) {
            String message = String.format("Uventet respons (%d) ved kall mot mot %s", response.code(), response.request().url().toString());
            log.error(message);
            throw new RuntimeException(message);
        }
    }

    public static String authHeaderMedInnloggetBruker() {
        return "Bearer " + SubjectHandler.getSsoToken().map(SsoToken::getToken).orElseThrow(() -> new RuntimeException("Fant ikke token til innlogget bruker"));
    }

}

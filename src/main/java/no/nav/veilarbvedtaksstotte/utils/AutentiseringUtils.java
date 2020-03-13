package no.nav.veilarbvedtaksstotte.utils;

import no.nav.common.auth.SsoToken;
import no.nav.common.auth.SubjectHandler;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;

import java.util.Optional;

public class AutentiseringUtils {

    private AutentiseringUtils(){}

    public static Optional<String> hentIdent() {
        return SubjectHandler.getIdent();
    }

    public static String createBearerToken() {
        return "Bearer " + SubjectHandler.getSsoToken().map(SsoToken::getToken).orElse("");
    }

    public static boolean erBeslutterForVedtak(String innloggetVeilederIdent, Vedtak vedtak) {
        return innloggetVeilederIdent != null && innloggetVeilederIdent.equals(vedtak.getBeslutterIdent());
    }

    public static boolean erAnsvarligVeilederForVedtak(String innloggetVeilederIdent, Vedtak vedtak) {
        return innloggetVeilederIdent != null && innloggetVeilederIdent.equals(vedtak.getVeilederIdent());
    }

}

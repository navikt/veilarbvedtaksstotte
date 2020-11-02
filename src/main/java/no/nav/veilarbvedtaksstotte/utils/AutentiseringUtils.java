package no.nav.veilarbvedtaksstotte.utils;

import no.nav.veilarbvedtaksstotte.domain.Vedtak;

public class AutentiseringUtils {

    private AutentiseringUtils(){}

    public static boolean erBeslutterForVedtak(String innloggetVeilederIdent, Vedtak vedtak) {
        return innloggetVeilederIdent != null && innloggetVeilederIdent.equals(vedtak.getBeslutterIdent());
    }

    public static boolean erAnsvarligVeilederForVedtak(String innloggetVeilederIdent, Vedtak vedtak) {
        return innloggetVeilederIdent != null && innloggetVeilederIdent.equals(vedtak.getVeilederIdent());
    }

}

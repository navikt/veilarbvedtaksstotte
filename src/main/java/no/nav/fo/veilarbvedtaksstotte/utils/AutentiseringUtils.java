package no.nav.fo.veilarbvedtaksstotte.utils;

import no.nav.common.auth.SubjectHandler;

import java.util.Optional;

public class AutentiseringUtils {

    public static Optional<String> hentIdent() {
        return SubjectHandler.getIdent();
    }

}

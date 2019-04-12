package no.nav.fo.veilarbvedtaksstotte.utils;

import javax.ws.rs.BadRequestException;

import static no.bekk.bekkopen.person.FodselsnummerValidator.isValid;

public class ValideringUtils {

    private ValideringUtils(){}

    public static void validerFnr(String fnr) {
        if (fnr == null || !isValid(fnr)) {
            throw new BadRequestException("Fnr mangler eller er ugyldig");
        }
    }

}

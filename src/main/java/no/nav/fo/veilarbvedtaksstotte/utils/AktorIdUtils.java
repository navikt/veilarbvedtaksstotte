package no.nav.fo.veilarbvedtaksstotte.utils;

import no.nav.dialogarena.aktor.AktorService;

public class AktorIdUtils {

    private AktorIdUtils() {}

    public static String getAktorIdOrThrow(AktorService aktorService, String fnr) {
        return aktorService.getAktorId(fnr).orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));
    }

}

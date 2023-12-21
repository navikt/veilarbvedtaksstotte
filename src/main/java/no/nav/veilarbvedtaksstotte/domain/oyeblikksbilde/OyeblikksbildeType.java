package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde;

import no.nav.veilarbvedtaksstotte.domain.arkiv.BrevKode;

import java.util.Arrays;

public enum OyeblikksbildeType {
    CV_OG_JOBBPROFIL,
    REGISTRERINGSINFO,
    EGENVURDERING;

    public static boolean contains(String value) {
        return Arrays.stream(OyeblikksbildeType.values()).anyMatch(x -> x.name().equals(value));
    }

    public static OyeblikksbildeType from(BrevKode brevKode) {
        return switch (brevKode) {
            case REGISTRERINGSINFO -> OyeblikksbildeType.REGISTRERINGSINFO;
            case CV_OG_JOBBPROFIL -> OyeblikksbildeType.CV_OG_JOBBPROFIL;
            case EGENVURDERING -> OyeblikksbildeType.EGENVURDERING;
        };
    }

}

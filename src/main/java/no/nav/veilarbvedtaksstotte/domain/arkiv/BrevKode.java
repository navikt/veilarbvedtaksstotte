package no.nav.veilarbvedtaksstotte.domain.arkiv;

import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;

import java.util.Arrays;

public enum BrevKode {
    CV_OG_JOBBPROFIL,
    REGISTRERINGSINFO,
    EGENVURDERING,
    ARBEIDSSOKERREGISTRET;

    public static BrevKode of(OyeblikksbildeType type) {
        return switch (type) {
            case REGISTRERINGSINFO -> BrevKode.REGISTRERINGSINFO;
            case CV_OG_JOBBPROFIL -> BrevKode.CV_OG_JOBBPROFIL;
            case EGENVURDERING -> BrevKode.EGENVURDERING;
            case ARBEIDSSOKERREGISTRET -> BrevKode.ARBEIDSSOKERREGISTRET;
        };
    }
    
    public static boolean contains(String value) {
        return Arrays.stream(BrevKode.values()).anyMatch(x -> x.name().equals(value));
    }
}

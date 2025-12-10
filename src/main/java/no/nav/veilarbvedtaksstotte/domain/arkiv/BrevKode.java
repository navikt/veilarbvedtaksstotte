package no.nav.veilarbvedtaksstotte.domain.arkiv;

import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;

public enum BrevKode {
    CV_OG_JOBBPROFIL,
    REGISTRERINGSINFO,
    EGENVURDERING,
    EGENVURDERING_V2,
    ARBEIDSSOKERREGISTRET;

    public static BrevKode of(OyeblikksbildeType type) {
        return switch (type) {
            case REGISTRERINGSINFO -> BrevKode.REGISTRERINGSINFO;
            case CV_OG_JOBBPROFIL -> BrevKode.CV_OG_JOBBPROFIL;
            case EGENVURDERING -> BrevKode.EGENVURDERING;
            case EGENVURDERING_V2 -> BrevKode.EGENVURDERING_V2;
            case ARBEIDSSOKERREGISTRET -> BrevKode.ARBEIDSSOKERREGISTRET;
        };
    }
}

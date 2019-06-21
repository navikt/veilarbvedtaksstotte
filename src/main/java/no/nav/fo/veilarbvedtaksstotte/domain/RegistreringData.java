package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Value;

@Value
public class RegistreringData {

    public enum ProfilertInnsatsgruppe {
        STANDARD_INNSATS,
        SITUASJONSBESTEMT_INNSATS,
        BEHOV_FOR_ARBEIDSEVNEVURDERING
    }

    @Value
    public static class BrukerRegistrering {
        public Profilering profilering;
    }

    @Value
    public static class Profilering {
        public ProfilertInnsatsgruppe innsatsgruppe;
    }

    public BrukerRegistrering registrering;

}

package no.nav.veilarbvedtaksstotte.client.norg2;

import lombok.Value;

@Value
public class EnhetPostboksadresse implements EnhetPostadresse {
    private final String postnummer;
    private final String poststed;
    private final String postboksnummer;
    private final String postboksanlegg;

    public String getPostnummer() {
        return this.postnummer;
    }

    public String getPoststed() {
        return this.poststed;
    }

    public String getPostboksnummer() {
        return this.postboksnummer;
    }

    public String getPostboksanlegg() {
        return this.postboksanlegg;
    }
}

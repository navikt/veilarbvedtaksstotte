package no.nav.veilarbvedtaksstotte.client.norg2;

import lombok.Value;

@Value
public class EnhetStedsadresse implements EnhetPostadresse {
    private final String postnummer;
    private final String poststed;
    private final String gatenavn;
    private final String husnummer;
    private final String husbokstav;
    private final String adresseTilleggsnavn;

    public String getPostnummer() {
        return this.postnummer;
    }

    public String getPoststed() {
        return this.poststed;
    }

    public String getGatenavn() {
        return this.gatenavn;
    }

    public String getHusnummer() {
        return this.husnummer;
    }

    public String getHusbokstav() {
        return this.husbokstav;
    }

    public String getAdresseTilleggsnavn() {
        return this.adresseTilleggsnavn;
    }
}

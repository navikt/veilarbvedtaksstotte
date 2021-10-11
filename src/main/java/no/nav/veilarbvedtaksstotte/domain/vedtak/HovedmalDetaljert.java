package no.nav.veilarbvedtaksstotte.domain.vedtak;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum HovedmalDetaljert {
    SKAFFE_ARBEID(Hovedmal.SKAFFE_ARBEID, "Skaffe arbeid"),
    BEHOLDE_ARBEID(Hovedmal.BEHOLDE_ARBEID, "Beholde arbeid");

    @JsonProperty("kode")
    Hovedmal kode;
    @JsonProperty("beskrivelse")
    String beskrivelse;

    HovedmalDetaljert(Hovedmal kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }
}

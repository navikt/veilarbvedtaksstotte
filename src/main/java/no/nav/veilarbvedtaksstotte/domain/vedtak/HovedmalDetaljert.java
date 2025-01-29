package no.nav.veilarbvedtaksstotte.domain.vedtak;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum HovedmalDetaljert {
    SKAFFE_ARBEID(HovedmalMedOkeDeltakelse.SKAFFE_ARBEID, "Skaffe arbeid"),
    BEHOLDE_ARBEID(HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID, "Beholde arbeid"),
    OKE_DELTAKELSE(HovedmalMedOkeDeltakelse.OKE_DELTAKELSE, "Øke deltakelse");

    @JsonProperty("kode")
    HovedmalMedOkeDeltakelse kode;
    @JsonProperty("beskrivelse")
    String beskrivelse;

    HovedmalDetaljert(HovedmalMedOkeDeltakelse kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }
}

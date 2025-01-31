package no.nav.veilarbvedtaksstotte.domain.vedtak;

import lombok.Getter;

@Getter
public enum HovedmalDetaljert {
    SKAFFE_ARBEID(HovedmalMedOkeDeltakelse.SKAFFE_ARBEID, "Skaffe arbeid"),
    BEHOLDE_ARBEID(HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID, "Beholde arbeid"),
    OKE_DELTAKELSE(HovedmalMedOkeDeltakelse.OKE_DELTAKELSE, "Øke deltakelse");

    final HovedmalMedOkeDeltakelse kode;
    final String beskrivelse;

    HovedmalDetaljert(HovedmalMedOkeDeltakelse kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }
}

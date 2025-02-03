package no.nav.veilarbvedtaksstotte.domain.vedtak;

public enum HovedmalDetaljert {
    SKAFFE_ARBEID(HovedmalMedOkeDeltakelse.SKAFFE_ARBEID, "Skaffe arbeid"),
    BEHOLDE_ARBEID(HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID, "Beholde arbeid"),
    OKE_DELTAKELSE(HovedmalMedOkeDeltakelse.OKE_DELTAKELSE, "Øke deltakelse");

    public HovedmalMedOkeDeltakelse getKode() {
        return kode;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    private final HovedmalMedOkeDeltakelse kode;
    private final String beskrivelse;

    HovedmalDetaljert(HovedmalMedOkeDeltakelse kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }
}

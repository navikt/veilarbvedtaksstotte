package no.nav.veilarbvedtaksstotte.domain.vedtak;

import lombok.Getter;

@Getter
public enum InnsatsgruppeDetaljert {
    STANDARD_INNSATS(Innsatsgruppe.STANDARD_INNSATS, "Gode muligheter", ArenaInnsatsgruppeKode.IKVAL),
    SITUASJONSBESTEMT_INNSATS(Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, "Trenger veiledning", ArenaInnsatsgruppeKode.BFORM),
    SPESIELT_TILPASSET_INNSATS(Innsatsgruppe.SPESIELT_TILPASSET_INNSATS, "Trenger veiledning, nedsatt arbeidsevne", ArenaInnsatsgruppeKode.BATT),
    GRADERT_VARIG_TILPASSET_INNSATS(Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS, "Jobbe delvis", ArenaInnsatsgruppeKode.VARIG),
    VARIG_TILPASSET_INNSATS(Innsatsgruppe.VARIG_TILPASSET_INNSATS, "Liten mulighet til å jobbe", ArenaInnsatsgruppeKode.VARIG);

    final Innsatsgruppe kode;
    final String beskrivelse;
    final ArenaInnsatsgruppeKode arenaKode;

    InnsatsgruppeDetaljert(Innsatsgruppe kode, String beskrivelse, ArenaInnsatsgruppeKode arenaKode) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
        this.arenaKode = arenaKode;
    }

    public enum ArenaInnsatsgruppeKode {
        BATT, BFORM, IKVAL, VARIG;
    }
}


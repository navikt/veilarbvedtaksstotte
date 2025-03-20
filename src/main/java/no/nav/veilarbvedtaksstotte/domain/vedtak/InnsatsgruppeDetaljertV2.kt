package no.nav.veilarbvedtaksstotte.domain.vedtak

enum class InnsatsgruppeDetaljertV2(
    val kode: InnsatsgruppeV2, val gammelKode: Innsatsgruppe, val arenaKode: ArenaInnsatsgruppeKode, val beskrivelse: String
) {
    GODE_MULIGHETER(
        InnsatsgruppeV2.GODE_MULIGHETER,
        Innsatsgruppe.STANDARD_INNSATS,
        ArenaInnsatsgruppeKode.IKVAL,
        "Gode muligheter"),
    TRENGER_VEILEDNING(
        InnsatsgruppeV2.TRENGER_VEILEDNING,
        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        ArenaInnsatsgruppeKode.BFORM,
        "Trenger veiledning"),
    TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE(
        InnsatsgruppeV2.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
        ArenaInnsatsgruppeKode.BATT,
        "Trenger veiledning, nedsatt arbeidsevne"),
    JOBBE_DELVIS(
        InnsatsgruppeV2.JOBBE_DELVIS,
        Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
        ArenaInnsatsgruppeKode.VARIG,
        "Jobbe delvis"),
    LITEN_MULIGHET_TIL_A_JOBBE(
        InnsatsgruppeV2.LITEN_MULIGHET_TIL_A_JOBBE,
        Innsatsgruppe.VARIG_TILPASSET_INNSATS,
        ArenaInnsatsgruppeKode.VARIG,
        "Liten mulighet til å jobbe");

    
    enum class ArenaInnsatsgruppeKode {
        BATT, BFORM, IKVAL, VARIG
    }
}

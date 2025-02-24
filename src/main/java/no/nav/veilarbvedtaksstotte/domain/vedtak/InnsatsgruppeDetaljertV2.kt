package no.nav.veilarbvedtaksstotte.domain.vedtak

enum class InnsatsgruppeDetaljertV2(
    val kode: InnsatsgruppeV2, val beskrivelse: String
) {
    GODE_MULIGHETER(
        InnsatsgruppeV2.GODE_MULIGHETER,
        "Gode muligheter"),
    TRENGER_VEILEDNING(
        InnsatsgruppeV2.TRENGER_VEILEDNING,
        "Trenger veiledning"),
    TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE(
        InnsatsgruppeV2.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
        "Trenger veiledning, nedsatt arbeidsevne"),
    JOBBE_DELVIS(
        InnsatsgruppeV2.JOBBE_DELVIS,
        "Jobbe delvis"),
    LITEN_MULIGHET_TIL_A_JOBBE(
        InnsatsgruppeV2.LITEN_MULIGHET_TIL_A_JOBBE,
        "Liten mulighet til å jobbe");
}

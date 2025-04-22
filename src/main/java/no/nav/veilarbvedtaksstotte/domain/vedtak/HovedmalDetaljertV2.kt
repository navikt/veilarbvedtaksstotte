package no.nav.veilarbvedtaksstotte.domain.vedtak

enum class HovedmalDetaljertV2(val kode: Hovedmal, val beskrivelse: String) {
    SKAFFE_ARBEID(Hovedmal.SKAFFE_ARBEID, "Skaffe arbeid"),
    BEHOLDE_ARBEID(Hovedmal.BEHOLDE_ARBEID, "Beholde arbeid")
}

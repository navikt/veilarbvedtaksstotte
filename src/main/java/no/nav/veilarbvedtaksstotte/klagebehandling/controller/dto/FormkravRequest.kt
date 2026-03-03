package no.nav.veilarbvedtaksstotte.klagebehandling.controller.dto

data class FormkravRequest(
    val vedtakId: Long,
    val signert: FormkravSvar,
    val part: FormkravSvar,
    val konkret: FormkravSvar,
    val klagefristOpprettholdt: FormkravSvar,
    val klagefristUnntak: FormkravKlagefristUnntakSvar?,
    val formkravBegrunnelseIntern: String?,
    val formkravBegrunnelseBrev: String?,
)


enum class FormkravSvar {
    JA, NEI
}

enum class FormkravKlagefristUnntakSvar {
    JA_KLAGER_KAN_IKKE_LASTES, JA_SAERLIGE_GRUNNER , NEI
}

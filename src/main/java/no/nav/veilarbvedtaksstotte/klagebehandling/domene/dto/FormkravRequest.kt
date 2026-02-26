package no.nav.veilarbvedtaksstotte.klagebehandling.domene.dto

data class FormkravRequest(
    val vedtakId: Long,
    val formkravOppfylt: Boolean,
    val formkravBegrunnelse: String?,
)

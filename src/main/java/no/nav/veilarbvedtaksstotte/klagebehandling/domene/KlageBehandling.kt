package no.nav.veilarbvedtaksstotte.klagebehandling.domene

import java.time.LocalDate

data class KlageBehandling(
    val vedtakId: Long,
    val veilederIdent: String,
    val norskIdent: String,
    val klageDato: LocalDate?,
    val klageJournalpostid: String?,
    val formkravOppfylt: FormkravOppfylt,
    val formkravBegrunnelse: String?,
    val resultat: Resultat,
    val resultatBegrunnelse: String?,
)

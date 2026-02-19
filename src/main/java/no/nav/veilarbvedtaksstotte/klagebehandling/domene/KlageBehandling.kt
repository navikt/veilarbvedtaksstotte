package no.nav.veilarbvedtaksstotte.klagebehandling.domene

import java.time.LocalDate

data class KlageBehandling(
    val vedtakId: Long,
    val veilederIdent: String,
    val norskIdent: String,
    val klageDato: LocalDate?,
    val klageBegrunnelse: String?
)

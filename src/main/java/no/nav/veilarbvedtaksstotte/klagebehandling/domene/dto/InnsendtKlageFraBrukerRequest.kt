package no.nav.veilarbvedtaksstotte.klagebehandling.domene.dto

import java.time.LocalDate

data class InnsendtKlageFraBrukerRequest(
    val vedtakId: Long,
    val klagedato: LocalDate?,
    val klageBegrunnelse: String?,
)

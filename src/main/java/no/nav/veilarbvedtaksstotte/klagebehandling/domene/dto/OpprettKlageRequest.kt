package no.nav.veilarbvedtaksstotte.klagebehandling.domene.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import no.nav.common.types.identer.Fnr
import java.time.LocalDate

data class OpprettKlageRequest(
    @field:NotNull
    val vedtakId: Long,
    @field:NotNull
    val fnr: Fnr,
    @field:NotBlank
    val veilederIdent: String,
    @field:NotNull
    val klagedato: LocalDate,
    @field:NotBlank
    val klageJournalpostid: String,
)

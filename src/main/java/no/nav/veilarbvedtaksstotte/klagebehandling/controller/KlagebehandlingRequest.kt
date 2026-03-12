package no.nav.veilarbvedtaksstotte.klagebehandling.controller

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import no.nav.common.types.identer.Fnr
import java.time.LocalDate

data class OppdaterFormkravRequest(
    val vedtakId: Long,
    val signert: FormkravSvar,
    val part: FormkravSvar,
    val konkret: FormkravSvar,
    val klagefristOpprettholdt: FormkravSvar,
    val klagefristUnntak: FormkravKlagefristUnntakSvar?,
    val formkravBegrunnelseIntern: String?,
    val formkravBegrunnelseBrev: String?,
)

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

data class HentKlageRequest(
    val vedtakId: Long
)

enum class FormkravSvar {
    JA, NEI
}


enum class FormkravKlagefristUnntakSvar {
    JA_KLAGER_KAN_IKKE_LASTES, JA_SAERLIGE_GRUNNER, NEI
}

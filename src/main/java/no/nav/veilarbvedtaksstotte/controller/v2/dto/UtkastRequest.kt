package no.nav.veilarbvedtaksstotte.controller.v2.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.common.types.identer.Fnr

data class UtkastRequest(
    @Schema(description = "Fødselsnummeret til brukeren som vedtaksutkastet er knyttet til")
    val fnr: Fnr
)

package no.nav.veilarbvedtaksstotte.controller.v2.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.common.types.identer.Fnr

data class UtrullingRequest(
    @Schema(description = "Fødselsnummeret til en oppfølgingsbruker")
    val fnr: Fnr
)

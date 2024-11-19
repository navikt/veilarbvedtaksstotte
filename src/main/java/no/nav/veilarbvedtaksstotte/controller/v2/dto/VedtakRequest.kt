package no.nav.veilarbvedtaksstotte.controller.v2.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.common.types.identer.Fnr

data class VedtakRequest(
    @Schema(description = "Fødselsnummeret til en brukeren § 14 a-vedtaket tilhører")
    val fnr: Fnr
)

package no.nav.veilarbvedtaksstotte.controller.v2.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.common.types.identer.Fnr

data class Siste14aVedtakRequest(
    @Schema(description = "Fødselsnummer til brukeren som man ønsker å hente det siste § 14 a-vedtaket til")
    val fnr: Fnr
)

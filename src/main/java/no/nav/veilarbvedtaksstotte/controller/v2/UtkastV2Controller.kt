package no.nav.veilarbvedtaksstotte.controller.v2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.veilarbvedtaksstotte.controller.dto.VedtakUtkastDTO
import no.nav.veilarbvedtaksstotte.controller.v2.dto.UtkastRequest
import no.nav.veilarbvedtaksstotte.mapper.toVedtakUtkastDTO
import no.nav.veilarbvedtaksstotte.service.VedtakService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2")
@Tag(
    name = "Utkast til § 14 a-vedtak V2",
    description = "Funksjonalitet knyttet til utkast til § 14 a-vedtak."
)
class UtkastV2Controller(
    val vedtakService: VedtakService
) {
    @PostMapping("/hent-utkast")
    @Operation(
        summary = "Hent utkast til § 14 a-vedtak",
        description = "Henter utkast til § 14 a-vedtak for en spesifisert bruker.",
        responses = [
            ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = VedtakUtkastDTO::class))]),
            ApiResponse(responseCode = "403", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "404", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", content = [Content(schema = Schema(hidden = true))])
        ]
    )
    fun hentUtkast(@RequestBody utkastRequest: UtkastRequest): VedtakUtkastDTO {
        return toVedtakUtkastDTO(vedtakService.hentUtkast(utkastRequest.fnr))
    }

    @PostMapping("/utkast/hent-harUtkast")
    @Operation(
        summary = "Har utkast til § 14 a-vedtak",
        description = "Sjekk om det eksisterer et utkast til § 14 a-vedtak for den spesifiserte brukeren.",
        responses = [
            ApiResponse(responseCode = "200", content = [Content(schema = Schema(implementation = Boolean::class))]),
            ApiResponse(responseCode = "403", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", content = [Content(schema = Schema(hidden = true))])
        ]
    )
    fun harUtkast(@RequestBody utkastRequest: UtkastRequest): Boolean {
        return vedtakService.harUtkast(utkastRequest.fnr)
    }
}

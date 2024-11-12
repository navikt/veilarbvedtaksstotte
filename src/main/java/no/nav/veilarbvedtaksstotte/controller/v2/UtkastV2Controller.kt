package no.nav.veilarbvedtaksstotte.controller.v2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.veilarbvedtaksstotte.controller.v2.dto.UtkastRequest
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.service.VedtakService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2")
@Tag(
        name = "Vedtaksutkast V2",
        description = "Funksjonalitet knyttet til vedtaksutkast."
)
class UtkastV2Controller(
    val vedtakService: VedtakService
) {
    @PostMapping("/hent-utkast")
    @Operation(
        summary = "Hent vedtaksutkast",
        description = "Henter vedtaksutkastet for en spesifisert bruker.",
        responses = [
            ApiResponse(responseCode = "403"),
            ApiResponse(responseCode = "404"),
            ApiResponse(responseCode = "500")
        ]
    )
    fun hentUtkast(@RequestBody utkastRequest: UtkastRequest): Vedtak {
        return vedtakService.hentUtkast(utkastRequest.fnr)
    }

    @PostMapping("/utkast/hent-harUtkast")
    @Operation(
        summary = "Har vedtaksutkast",
        description = "Sjekk om det eksisterer et vedtaksutkast for den spesifiserte brukeren.",
        responses = [
            ApiResponse(responseCode = "403"),
            ApiResponse(responseCode = "500")
        ]
    )
    fun harUtkast(@RequestBody utkastRequest: UtkastRequest): Boolean {
        return vedtakService.harUtkast(utkastRequest.fnr)
    }
}

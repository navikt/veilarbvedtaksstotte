package no.nav.veilarbvedtaksstotte.controller.v2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.annotations.EksterntEndepunkt
import no.nav.veilarbvedtaksstotte.controller.dto.Siste14aVedtakDTO
import no.nav.veilarbvedtaksstotte.controller.v2.dto.Siste14aVedtakRequest
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.Siste14aVedtakService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v2")
@Tag(
    name = "Siste § 14 a-vedtak V2",
    description = "Funksjonalitet knyttet til siste § 14 a-vedtak."
)
class Siste14aVedtakV2Controller(
    val authService: AuthService,
    val siste14aVedtakService: Siste14aVedtakService
) {

    @EksterntEndepunkt
    @PostMapping("/hent-siste-14a-vedtak")
    @Operation(
        summary = "Hent siste 14a vedtak",
        description = "Henter det siste registrerte § 14 a-vedtaket for den spesifiserte brukeren.",
        responses = [
            ApiResponse(
                responseCode = "200",
                content = [Content(schema = Schema(implementation = Siste14aVedtakDTO::class))]
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun hentSiste14aVedtak(@RequestBody siste14aVedtakRequest: Siste14aVedtakRequest): Siste14aVedtakDTO? {
        sjekkTilgang(siste14aVedtakRequest.fnr)

        return siste14aVedtakService.siste14aVedtak(siste14aVedtakRequest.fnr)
            ?.let { Siste14aVedtakDTO.fraSiste14aVedtak(it) }
    }

    private fun sjekkTilgang(fnr: Fnr) {
        if (authService.erSystemBruker()) {
            if (!authService.harSystemTilSystemTilgangMedEkstraRolle("siste-14a-vedtak")) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN)
            }
        } else if (authService.erEksternBruker()) {
            authService.sjekkEksternbrukerTilgangTilBruker(fnr)
        } else {
            authService.sjekkVeilederTilgangTilBruker(fnr)
        }
    }
}

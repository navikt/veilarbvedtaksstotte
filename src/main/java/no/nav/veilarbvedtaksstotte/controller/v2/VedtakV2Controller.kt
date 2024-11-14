package no.nav.veilarbvedtaksstotte.controller.v2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.veilarbvedtaksstotte.controller.v2.dto.VedtakRequest
import no.nav.veilarbvedtaksstotte.domain.arkiv.ArkivertVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.service.ArenaVedtakService
import no.nav.veilarbvedtaksstotte.service.VedtakService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/vedtak")
@Tag(
    name = "Vedtak V2",
    description = "Funksjonalitet knyttet til § 14 a-vedtak."
)
class VedtakV2Controller(
    val vedtakService: VedtakService,
    val arenaVedtakService: ArenaVedtakService,
) {
    @PostMapping("/hent-fattet")
    @Operation(
        summary = "Hent fattede § 14 a-vedtak",
        description = "Henter fattede § 14 a-vedtak for den spesifiserte brukeren.",
        responses = [
            ApiResponse(
                responseCode = "200",
                content = [Content(array = ArraySchema(schema = Schema(implementation = Vedtak::class)))]
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun hentFattedeVedtak(@RequestBody vedtakRequest: VedtakRequest): List<Vedtak> {
        return vedtakService.hentFattedeVedtak(vedtakRequest.fnr)
    }

    @PostMapping("/hent-arena")
    @Operation(
        summary = "Hent § 14 a-vedtak fra Arena",
        description = "Henter arkiverte § 14 a-vedtak fra Arena for den spesifiserte brukeren.",
        responses = [
            ApiResponse(
                responseCode = "200",
                content = [Content(array = ArraySchema(schema = Schema(implementation = ArkivertVedtak::class)))]
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "500",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun hentVedtakFraArena(@RequestBody vedtakRequest: VedtakRequest): List<ArkivertVedtak> {
        return arenaVedtakService.hentVedtakFraArena(vedtakRequest.fnr)
    }
}

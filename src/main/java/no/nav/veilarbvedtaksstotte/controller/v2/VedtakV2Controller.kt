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
import no.nav.veilarbvedtaksstotte.service.UtrullingService
import no.nav.veilarbvedtaksstotte.service.VedtakService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/vedtak")
@Tag(
    name = "Fattede § 14 a-vedtak V2",
    description = "Funksjonalitet knyttet til fattede § 14 a-vedtak."
)
class VedtakV2Controller(
    val vedtakService: VedtakService,
    val arenaVedtakService: ArenaVedtakService,
    private val utrullingService: UtrullingService,
) {
    @PostMapping("/hent-fattet")
    @Operation(
        summary = "Hent fattede § 14 a-vedtak",
        description = "Henter fattede § 14 a-vedtak for den spesifiserte brukeren hvor ny vedtaksløsning for § 14 a (denne applikasjonen) er kilden.",
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
        utrullingService.sjekkAtBrukerTilhorerUtrulletKontor(vedtakRequest.fnr)

        return vedtakService.hentFattedeVedtak(vedtakRequest.fnr)
    }

    @PostMapping("/hent-arena")
    @Operation(
        summary = "Hent fattede (arkiverte) § 14 a-vedtak fra Arena",
        description = "Henter metadata om fattede (arkiverte) § 14 a-vedtak for den spesifiserte brukeren hvor Arena er kilden.",
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
        utrullingService.sjekkAtBrukerTilhorerUtrulletKontor(vedtakRequest.fnr);

        return arenaVedtakService.hentVedtakFraArena(vedtakRequest.fnr)
    }
}

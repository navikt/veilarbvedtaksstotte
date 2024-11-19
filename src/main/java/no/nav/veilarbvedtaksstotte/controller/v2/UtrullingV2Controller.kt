package no.nav.veilarbvedtaksstotte.controller.v2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.veilarbvedtaksstotte.controller.v2.dto.UtrullingRequest
import no.nav.veilarbvedtaksstotte.service.UtrullingService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/utrulling")
@Tag(
    name = "Utrulling V2",
    description = "Funksjonalitet knyttet til utrulling av vedtaksløsningen for § 14 a (hvilke kontorer som har tilgang til løsningen)."
)
class UtrullingV2Controller(
    val utrullingService: UtrullingService
) {

    @PostMapping("/hent-tilhorerBrukerUtrulletKontor")
    @Operation(
        summary = "Tilhører bruker utrullet enhet",
        description = "Sjekker om spesifisert bruker tilhører en NAV-enhet hvor løsningen for § 14 a-vedtak er rullet ut.",
        responses = [
            ApiResponse(
                responseCode = "200",
                content = [Content(schema = Schema(implementation = Boolean::class))]
            ),
        ]
    )
    fun tilhorerBrukerUtrulletKontor(@RequestBody utrullingRequest: UtrullingRequest): Boolean {
        return utrullingService.tilhorerBrukerUtrulletKontor(utrullingRequest.fnr)
    }
}

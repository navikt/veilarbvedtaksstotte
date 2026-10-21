package no.nav.veilarbvedtaksstotte.controller

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.utils.MODIA_KVALITETSSIKRING_OPPFOLGINGSVEDTAK
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1/tilgang")
@Tag(
    name = "Tilgang",
    description = "Funksjonalitet knyttet til tilgangskontroll for veiledere."
)
class TilgangController(
    private val authService: AuthService,
    private val unleashService: DefaultUnleash
) {
    @PostMapping("/hent-tilgang")
    fun hentVeilederTilgang(@Valid @RequestBody hentTilgangRequest: HentTilgangRequest): Boolean {
        val unleashContext = UnleashContext.builder()
            .userId(authService.innloggetVeilederIdent)
            .build()

        return try {
            if (unleashService.isEnabled(MODIA_KVALITETSSIKRING_OPPFOLGINGSVEDTAK, unleashContext)) {
                authService.sjekkVeilederTilgangTilBruker(TilgangType.LESE, hentTilgangRequest.brukerId)
                true
            } else {
                authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, hentTilgangRequest.brukerId)
                true
            }
        } catch (e: ResponseStatusException) {
            if(e.statusCode == HttpStatus.FORBIDDEN) {
                return false
            } else {
                throw e
            }
        }
    }
}

data class BrukerIdRequest(
    @field:NotNull
    val brukerId: Fnr
)
typealias HentTilgangRequest = BrukerIdRequest

package no.nav.veilarbvedtaksstotte.controller

import io.getunleash.DefaultUnleash
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NorskIdent
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarbvedtaksstotte.client.tilgangsmaskin.TilgangsmaskinClient
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.OppfolgingsenhetService
import no.nav.veilarbvedtaksstotte.utils.TILGANGSMASKIN_INTEGRASJON
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/tilgang")
@ConditionalOnProperty(
    prefix = "app.env",
    name = ["tilgangsmaskinEnkelttilgangEnabled"],
    havingValue = "true"
)
@Tag(
    name = "Enkelttilgang",
    description = "Registrering av enkelttilgang i testmiljøet. Krever Unleash-bryteren " +
        "veilarbvedtaksstotte.tilgangsmaskin_integrasjon; svarer med 404 når bryteren er av."
)
class TilgangController(
    private val tilgangsmaskinClient: TilgangsmaskinClient,
    private val authService: AuthService,
    private val oppfolgingsenhetService: OppfolgingsenhetService,
    private val unleashService: DefaultUnleash
) {
    companion object {
        private const val BEGRUNNELSE = "Kvalitetssikring av oppfølgingsvedtak § 14 a."
    }

    @PostMapping("/enkelttilgang")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Registrer enkelttilgang",
        description = "Registrerer enkelttilgang for innlogget veileder og angitt bruker i testmiljøet.",
        responses = [
            ApiResponse(responseCode = "204", description = "Enkelttilgang registrert"),
            ApiResponse(responseCode = "400", description = "Ugyldig brukerident"),
            ApiResponse(responseCode = "403", description = "Innlogget veileder mangler NAV-ident"),
            ApiResponse(responseCode = "404", description = "Funksjonsbryteren er av")
        ]
    )
    fun opprettEnkelttilgang(@Valid @RequestBody request: OpprettEnkelttilgangRequest) {
        sjekkFunksjonsbryter()
        authService.sjekkInternBruker()
        authService.sjekkTilgangTilModia()

        tilgangsmaskinClient.opprettEnkelttilgang(
            authService.innloggetVeilederIdent,
            NorskIdent.of(request.brukerId.get()),
            BEGRUNNELSE,
            LocalDate.now().plusDays(1)
        )
    }

    @PostMapping("/hent-tilgang")
    fun hentVeilederTilgang(@Valid @RequestBody hentTilgangRequest: HentTilgangRequest): Boolean {
        sjekkFunksjonsbryter()
        authService.sjekkInternBruker()
        authService.sjekkTilgangTilModia()

        // TODO: Midlertidig disabling av enhetstilgangssjekk i dev
        return if (EnvironmentUtils.isProduction().orElse(true) ?: true) {
            try {
                val enhet = oppfolgingsenhetService.hentOppfolgingsenhet(hentTilgangRequest.brukerId)
                    .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Enhet er ikke satt på bruker") }
                authService.sjekkTilgangTilEnhet(enhet)
                true
            } catch (_: ResponseStatusException) {
                false
            }
        } else true
    }

    private fun sjekkFunksjonsbryter() {
        if (!unleashService.isEnabled(TILGANGSMASKIN_INTEGRASJON)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
    }
}

data class BrukerIdRequest(
    @field:NotNull
    val brukerId: Fnr
)
typealias OpprettEnkelttilgangRequest = BrukerIdRequest
typealias HentTilgangRequest = BrukerIdRequest

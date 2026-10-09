package no.nav.veilarbvedtaksstotte.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NorskIdent
import no.nav.veilarbvedtaksstotte.client.tilgangsmaskin.TilgangsmaskinClient
import no.nav.veilarbvedtaksstotte.service.AuthService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/enkelttilganger")
@ConditionalOnProperty(
    prefix = "app.env",
    name = ["tilgangsmaskinEnkelttilgangEnabled"],
    havingValue = "true"
)
@Tag(
    name = "Enkelttilgang",
    description = "Registrering av enkelttilgang i testmiljøet."
)
class EnkelttilgangController(
    private val tilgangsmaskinClient: TilgangsmaskinClient,
    private val authService: AuthService
) {
    companion object {
        private const val BEGRUNNELSE = "Arbeidsrettet oppfølging i veilarbvedtaksstotte"
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Registrer enkelttilgang",
        description = "Registrerer enkelttilgang for innlogget veileder og angitt bruker i testmiljøet.",
        responses = [
            ApiResponse(responseCode = "204", description = "Enkelttilgang registrert"),
            ApiResponse(responseCode = "400", description = "Ugyldig brukerident"),
            ApiResponse(responseCode = "403", description = "Innlogget veileder mangler NAV-ident")
        ]
    )
    fun opprettEnkelttilgang(@Valid @RequestBody request: OpprettEnkelttilgangRequest) {
        authService.sjekkTilgangTilModia()

        tilgangsmaskinClient.opprettEnkelttilgang(
            authService.innloggetVeilederIdent,
            NorskIdent.of(request.brukerId.get()),
            BEGRUNNELSE,
            LocalDate.now()
        )
    }
}

data class OpprettEnkelttilgangRequest(
    @field:NotNull
    val brukerId: Fnr
)

package no.nav.veilarbvedtaksstotte.controller

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NorskIdent
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.client.tilgangsmaskin.TilgangsmaskinClient
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.utils.MODIA_KVALITETSSIKRING_OPPFOLGINGSVEDTAK
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.ZonedDateTime

@RestController
@RequestMapping("/api/v1/tilgang")
@Tag(
    name = "Tilgang",
    description = "Funksjonalitet knyttet til tilgangskontroll for veiledere."
)
class TilgangController(
    private val authService: AuthService,
    private val unleashService: DefaultUnleash,
    private val tilgangsmaskinClient: TilgangsmaskinClient,
) {
    @PostMapping("/hent-tilgang")
    fun hentVeilederTilgang(@Valid @RequestBody hentTilgangRequest: HentTilgangRequest): Boolean {
        val unleashContext = UnleashContext.builder()
            .userId(authService.innloggetVeilederIdent)
            .build()

        return try {
            if (unleashService.isEnabled(MODIA_KVALITETSSIKRING_OPPFOLGINGSVEDTAK, unleashContext)) {
                authService.sjekkVeilederTilgangTilBruker(TilgangType.LESE, Fnr.of(hentTilgangRequest.brukerId.get()))
                true
            } else {
                authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.LESE, Fnr.of(hentTilgangRequest.brukerId.get()))
                true
            }
        } catch (e: ResponseStatusException) {
            if (e.statusCode == HttpStatus.FORBIDDEN) {
                return false
            } else {
                throw e
            }
        }
    }

    @PostMapping("/enkelttilgang")
    fun opprettEnkelttilgang(@Valid @RequestBody request: OpprettEnkelttilgangRequest) {
        val unleashContext = UnleashContext.builder()
            .userId(authService.innloggetVeilederIdent)
            .build()

        if (!unleashService.isEnabled(MODIA_KVALITETSSIKRING_OPPFOLGINGSVEDTAK, unleashContext)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        authService.sjekkInternBruker()
        authService.sjekkModiaGenerellTilgang()

        tilgangsmaskinClient.opprettEnkelttilgang(
            authService.innloggetVeilederIdent,
            request.brukerId,
            BEGRUNNELSE,
            // TODO: Kor lang gyldigheit vil vi/kan vi forsvare å ha?
            ZonedDateTime.now().plusDays(1).toLocalDate()
        )
    }

    companion object {
        private const val BEGRUNNELSE = "Kvalitetssikring av oppfølgingsvedtak § 14 a."
    }
}

data class BrukerIdRequest(
    @field:NotNull
    val brukerId: NorskIdent
)
typealias HentTilgangRequest = BrukerIdRequest
typealias OpprettEnkelttilgangRequest = BrukerIdRequest

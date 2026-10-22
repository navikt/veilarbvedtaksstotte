package no.nav.veilarbvedtaksstotte.controller

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.common.types.identer.EnhetId
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSok
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BrukereMedAntall
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.BeslutteroversiktService
import no.nav.veilarbvedtaksstotte.utils.MODIA_KVALITETSSIKRING_OPPFOLGINGSVEDTAK
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/beslutteroversikt")
@Tag(
    name = "Kvalitetssikringsoversikt",
    description = "Funksjonalitet knyttet til kvalitetssikringsoversikten. Kvalitetssikringsoversikten gir mulighet " +
            "for kvalitetssikrere å følge opp utkast til § 14 a-vedtak som krever kvalitetssikring."
)
class BeslutteroversiktController @Autowired constructor(
    private val beslutteroversiktService: BeslutteroversiktService,
    private val unleashService: DefaultUnleash,
    private val authService: AuthService
) {
    @PostMapping("/sok")
    @Operation(
        summary = "Søk",
        description = """
                    Søker etter utkast til § 14 a-vedtak som krever kvalitetssikring.
                    Søket støtter paginering, sortering og følgende filtreringsmuligheter:
                    
                    * filtrering på en liste med Nav-enheter
                      * default oppførsel er å hente utkast for alle Nav-enheter som autentisert kvalitetssikrer har tilgang til
                    * filtrering på kvalitetssikringsstatusen til utkastet
                      * default oppførsel er å inkludere alle utkast uavhengig av status
                    * filtrering på autentisert kvalitetssikrer egne brukere
                      * default oppførsel er å inkludere alle utkast for alle brukere på alle Nav-enhetene som autentisert kvalitetssikrer har tilgang til
                    
                    Se `BeslutteroversiktSok`-modellen for detaljert beskrivelse av de ulike parametrene.
                    """,
        responses = [
            ApiResponse(
                responseCode = "200",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = BrukereMedAntall::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "404",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "500",
                content = [Content(schema = Schema(hidden = true))]
            )]
    )
    fun startBeslutterProsess(@RequestBody sokData: BeslutteroversiktSok): BrukereMedAntall? {
        return beslutteroversiktService.sokEtterBruker(sokData)
    }

    @GetMapping("/enheter")
    @Operation(
        summary = "Hent enheter med utkast til kvalitetssikring",
        description = """
                    Henter alle enheter som nå har utkast med status TRENGER_BESLUTTER,
                    KLAR_TIL_BESLUTTER eller KLAR_TIL_VEILEDER, også utkast sendt tilbake til veileder.
                    Godkjente utkast er ikke inkludert.
                    Hver enhet returneres én gang med enhetId og navn.
                    Enhetene deles i medTilgang og utenTilgang basert på innlogget veileders enhetstilgang.
                    Begge listene er sortert på enhetId.
                    Nav Vikafossen (2103) inkluderes bare hvis veilederen har tilgang til kode 6-brukere.
                    Krever intern bruker med tilgang til Modia.
                    Endepunktet er bare tilgjengelig i dev.
                    Krever at Unleash-bryteren veilarbvedtaksstotte.tilgangsmaskin_integrasjon er på.
                    
                    """,
        responses = [
            ApiResponse(
                responseCode = "200",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = ArraySchema(arraySchema = Schema(implementation = EnhetId::class))
                )]
            ),
            ApiResponse(
                responseCode = "400",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "404",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "500",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun hentEnheterMedUtkast(): List<EnhetId> {
        val unleashContext = UnleashContext.builder()
            .userId(authService.innloggetVeilederIdent)
            .build()

        if (!unleashService.isEnabled(MODIA_KVALITETSSIKRING_OPPFOLGINGSVEDTAK, unleashContext)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        return beslutteroversiktService.hentEnheterMedUtkast()
    }
}

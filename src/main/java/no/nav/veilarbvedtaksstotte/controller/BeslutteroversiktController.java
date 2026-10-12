package no.nav.veilarbvedtaksstotte.controller;

import io.getunleash.DefaultUnleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BrukereMedAntall;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.EnheterMedUtkast;
import no.nav.veilarbvedtaksstotte.service.AuthService;
import no.nav.veilarbvedtaksstotte.service.BeslutteroversiktService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.veilarbvedtaksstotte.utils.UnleashUtilsKt.TILGANGSMASKIN_INTEGRASJON;

@RestController
@RequestMapping("/api/beslutteroversikt")
@Tag(
        name = "Kvalitetssikringsoversikt",
        description = "Funksjonalitet knyttet til kvalitetssikringsoversikten. Kvalitetssikringsoversikten gir mulighet " +
                "for kvalitetssikrere å følge opp utkast til § 14 a-vedtak som krever kvalitetssikring."
)
public class BeslutteroversiktController {

    private final BeslutteroversiktService beslutteroversiktService;
    private final AuthService authService;
    private final DefaultUnleash unleashService;

    @Autowired
    public BeslutteroversiktController(BeslutteroversiktService beslutteroversiktService, AuthService authService,
                                       DefaultUnleash unleashService) {
        this.beslutteroversiktService = beslutteroversiktService;
        this.authService = authService;
        this.unleashService = unleashService;
    }

    @GetMapping(value = "/enheter", produces = MediaType.APPLICATION_JSON_VALUE)
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
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EnheterMedUtkast.class)
                    )),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public EnheterMedUtkast hentEnheterMedUtkast() {
        if (!unleashService.isEnabled(TILGANGSMASKIN_INTEGRASJON)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!isDevelopment().orElse(false)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Endepunktet er bare tilgjengelig i dev");
        }
        authService.sjekkInternBruker();
        authService.sjekkTilgangTilModia();
        return beslutteroversiktService.hentEnheterMedUtkast();
    }

    @PostMapping("/sok")
    @Operation(
            summary = "Søk",
            description = """
                    Søker etter utkast til § 14 a-vedtak som krever kvalitetssikring.
                    Med Unleash-bryteren veilarbvedtaksstotte.tilgangsmaskin_integrasjon på
                    kreves intern bruker med tilgang til Modia, og ny tilgangsfunksjonalitet i dev aktiveres.
                    Med bryteren av brukes tidligere enhetsfilter og persontilgang.
                    Søket støtter paginering, sortering og følgende filtreringsmuligheter:
                    
                    * filtrering på en liste med Nav-enheter
                      * default oppførsel er å hente utkast for alle Nav-enheter som autentisert kvalitetssikrer har tilgang til
                      * i dev kan enheter velges selv uten enhetstilgang; i andre miljøer kreves enhetstilgang
                      * i dev krever søk som inkluderer Nav Vikafossen (2103) tilgang til kode 6-brukere
                    * filtrering på kvalitetssikringsstatusen til utkastet
                      * default oppførsel er å inkludere alle utkast uavhengig av status
                    * filtrering på autentisert kvalitetssikrer egne brukere
                      * default oppførsel er å inkludere alle utkast for alle brukere på alle Nav-enhetene som autentisert kvalitetssikrer har tilgang til
                    
                    Se `BeslutteroversiktSok`-modellen for detaljert beskrivelse av de ulike parametrene.
                    Navn og fødselsnummer skjules for brukere kvalitetssikreren ikke har persontilgang til.
                    I dev vurderes persontilgang med kjerneregler uten enhetstilgang.
                    Oppfølgingstilgang, adressebeskyttelse og skjerming kontrolleres fortsatt.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = BrukereMedAntall.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public BrukereMedAntall sokEtterUtkastTilKvalitetssikring(@RequestBody BeslutteroversiktSok sokData) {
        if (unleashService.isEnabled(TILGANGSMASKIN_INTEGRASJON)) {
            return sokEtterUtkastTilKvalitetssikringMedTilgangsmaskin(sokData);
        } else {
            return sokEtterUtkastTilKvalitetssikringGammel(sokData);
        }
    }

    private BrukereMedAntall sokEtterUtkastTilKvalitetssikringMedTilgangsmaskin(BeslutteroversiktSok sokData) {
        authService.sjekkInternBruker();
        authService.sjekkTilgangTilModia();
        return beslutteroversiktService.sokEtterBruker(sokData);
    }

    private BrukereMedAntall sokEtterUtkastTilKvalitetssikringGammel(BeslutteroversiktSok sokData) {
        return beslutteroversiktService.sokEtterBruker(sokData);
    }
}

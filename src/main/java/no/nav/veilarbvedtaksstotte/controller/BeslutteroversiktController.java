package no.nav.veilarbvedtaksstotte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BrukereMedAntall;
import no.nav.veilarbvedtaksstotte.service.BeslutteroversiktService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beslutteroversikt")
@Tag(
        name = "Kvalitetssikringsoversikt",
        description = "Funksjonalitet knyttet til kvalitetssikringsoversikten. Kvalitetssikringsoversikten gir mulighet " +
                "for kvalitetssikrere å følge opp utkast til § 14 a-vedtak som krever kvalitetssikring."
)
public class BeslutteroversiktController {

    private final BeslutteroversiktService beslutteroversiktService;

    @Autowired
    public BeslutteroversiktController(BeslutteroversiktService beslutteroversiktService) {
        this.beslutteroversiktService = beslutteroversiktService;
    }

    @PostMapping("/sok")
    @Operation(
            summary = "Søk",
            description = """
                    Søker etter utkast til § 14 a-vedtak som krever kvalitetssikring. " +
                    Søket støtter paginering, sortering og følgende filtreringsmuligheter:
                    
                    * filtrering på en liste med Nav-enheter
                      * default oppførsel er å hente utkast for alle Nav-enheter som autentisert kvalitetssikrer har tilgang til
                    * filtrering på kvalitetssikringsstatusen til utkastet
                      * default oppførsel er å inkludere alle utkast uavhengig av status
                    * filtrering på autentisert kvalitetssikrer egne brukere
                      * default oppførsel er å inkludere alle utkast for alle brukere på alle Nav-enhetene som autentisert kvalitetssikrer har tilgang til
                    * filtrering på navn eller fødselsnummer på bruker som utkastet er knyttet til
                      * default oppførsel er å ikke filtrere på navn eller fødselsnummer
                    
                    Se `BeslutteroversiktSok`-modellen for detaljert beskrivelse av de ulike parametrene.
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
    public BrukereMedAntall startBeslutterProsess(@RequestBody BeslutteroversiktSok sokData) {
        return beslutteroversiktService.sokEtterBruker(sokData);
    }

}

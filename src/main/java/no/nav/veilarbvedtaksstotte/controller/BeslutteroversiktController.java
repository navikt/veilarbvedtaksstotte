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
        description = "Funksjonalitet knyttet til kvalitetssikringsoversikten."
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
            description = "Søker etter brukere og tilhørende vedtaksutkast som krever kvalitetssikring. Søket vil returnere brukere fra samtlige enheter som innlogget/autentisert bruker (veileder) er knyttet til og har tilgang til, med mindre en liste av enheter som det skal filtreres på oppgis i requesten.",
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

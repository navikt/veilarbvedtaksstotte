package no.nav.veilarbvedtaksstotte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.veilarbvedtaksstotte.service.BeslutterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beslutter")
@Tag(name = "Kvalitetssikring", description = "Funksjonalitet knyttet til kvalitetssikring og kvalitetssikrere.")
public class BeslutterController {

    private final BeslutterService beslutterService;

    @Autowired
    public BeslutterController(BeslutterService beslutterService) {
        this.beslutterService = beslutterService;
    }

    @PostMapping("/start")
    @Operation(
            summary = "Start kvalitetssikring",
            description = "Starter kvalitetssikring for det spesifiserte vedtaksutkastet. " +
                    "Informasjon om brukeren som er knyttet til vedtaksutkastet samt øvrig informasjon fra vedtaksutkastet " +
                    "vil bli lagt til i kvalitetssikringsoversikten og en systemmelding om at kvalitetssikring er startet vil bli produsert" +
                    "og postet i meldingskanalen mellom ansvarlig veileder og kvalitetssikrer.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content()),
                    @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public void startBeslutterProsess(@RequestParam("vedtakId") long vedtakId) {
        beslutterService.startBeslutterProsess(vedtakId);
    }

    @PostMapping("/avbryt")
    @Operation(
            summary = "Avbryt kvalitetssikring",
            description = "Avbryter kvalitetssikring for det spesifiserte vedtaksutkastet. " +
                    "Informasjon om brukeren som er knyttet til vedtaksutkastet samt øvrig informasjon fra vedtaksutkastet " +
                    "vil bli fjernet fra kvalitetssikringsoversikten og en systemmelding om at kvalitetssikring er avbrutt vil bli produsert" +
                    "og postet i meldingskanalen mellom ansvarlig veileder og kvalitetssikrer.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content()),
                    @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public void avbrytBeslutterProsess(@RequestParam("vedtakId") long vedtakId) {
        beslutterService.avbrytBeslutterProsess(vedtakId);
    }

    @PostMapping("/bliBeslutter")
    @Operation(
            summary = "Bli kvalitetssikrer",
            description = "Innlogget/autentisert bruker (veileder) blir kvalitetssikrer for det spesifiserte vedtaksutkastet." +
                    "Kvalitetssikringsoversikten vil bli oppdatert med informasjon om kvalitetssikreren og en systemmelding om " +
                    "at vedtakutkastet har fått en kvalitetssikrer vil bli produsert og postet i meldingskanalen mellom ansvarlig " +
                    "veileder og kvalitetssikrer",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content()),
                    @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public void bliBeslutter(@RequestParam("vedtakId") long vedtakId) {
        beslutterService.bliBeslutter(vedtakId);
    }

    @PostMapping("/godkjenn")
    @Operation(
            summary = "Godkjenn vedtaksutkast",
            description = "Godkjenn det spesifiserte vedtaksutkastet. Informasjon om brukeren som er knyttet til vedtaksutkastet " +
                    "samt øvrig informasjon fra vedtaksutkastet vil bli fjernet fra kvalitetssikringsoversikten og en systemmelding " +
                    "om at vedtaksutkastet er godkjent vil bli produsert og postet i meldingskanalen mellom ansvarlig veileder og kvalitetssikrer.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content()),
                    @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public void godkjennVedtak(@RequestParam("vedtakId") long vedtakId) {
        beslutterService.setGodkjentAvBeslutter(vedtakId);
    }

    @PutMapping("/status")
    @Operation(
            summary = "Oppdater status på kvalitetssikringen",
            description = """
                    Oppdaterer og setter ny status for kvalitetssikringen:
                    
                    * dersom innlogget/autentisert bruker er ansvarlig veileder for vedtaksutkastet vil ny status bli "Klar til kvalitetssikrer"
                    * dersom innlogget/autentisert bruker er kvalitetssikrer for vedtaksutkastet vil ny status bli "Klar til ansvarlig veileder"
                    
                    Kvalitetssikringsoversikten vil bli oppdatert med informasjon om ny status og en systemmelding om at vedtakutkastet har fått en ny status bli produsert og postet i meldingskanalen mellom ansvarlig veileder og kvalitetssikrer.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content()),
                    @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public void oppdaterBeslutterProsessStatus(@RequestParam("vedtakId") long vedtakId) {
        beslutterService.oppdaterBeslutterProsessStatus(vedtakId);
    }

}

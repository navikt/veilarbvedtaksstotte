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
            description = """
                    Starter kvalitetssikring for det spesifiserte utkastet til § 14 a-vedtak. Følgende tilleggssteg vil utføres:
                    
                    * informasjon fra utkastet og om brukeren brukeren det er knyttet til vil bli lagt til i kvalitetssikringsoversikten
                    * det produseres en systemmelding om at kvalitetssikring er startet
                    * systemmeldingen postes i meldingskanalen mellom ansvarlig veileder og kvalitetssikrer
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content()),
                    @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public void startBeslutterProsess(@RequestParam("vedtakId") long vedtakId) {
        // Sjekkar utrulling for kontoret til brukar ✅

		beslutterService.startBeslutterProsess(vedtakId);
	}

    @PostMapping("/avbryt")
    @Operation(
            summary = "Avbryt kvalitetssikring",
            description = """
                    Avbryter kvalitetssikring for det spesifiserte utkastet for § 14 a-vedtak. Følgende tilleggssteg vil utføres:
                    
                    * informasjon fra utkastet og om brukeren brukeren det er knyttet til vil bli fjernet fra kvalitetssikringsoversikten
                    * det produseres en systemmelding om at kvalitetssikring er avbrutt
                    * systemmeldingen postes i meldingskanalen mellom ansvarlig veileder og kvalitetssikrer
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content()),
                    @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public void avbrytBeslutterProsess(@RequestParam("vedtakId") long vedtakId) {
        // Sjekkar utrulling for kontoret til brukar ✅

		beslutterService.avbrytBeslutterProsess(vedtakId);
	}

    @PostMapping("/bliBeslutter")
    @Operation(
            summary = "Bli kvalitetssikrer",
            description = """
                    Innlogget/autentisert veileder blir kvalitetssikrer for det spesifiserte utkastet for § 14 a-vedtak. Følgende tilleggssteg vil utføres:
                    
                    * kvalitetssikringsoversikten blir oppdatert med informasjon om kvalitetssikreren
                    * det produseres en systemmelding om at utkastet har fått en kvalitetssikrer
                    * systemmeldingen postes i meldingskanalen mellom ansvarlig veileder og kvalitetssikrer
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content()),
                    @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public void bliBeslutter(@RequestParam("vedtakId") long vedtakId) {
        // Sjekkar utrulling for kontoret til brukar ✅

		beslutterService.bliBeslutter(vedtakId);
	}

    @PostMapping("/godkjenn")
    @Operation(
            summary = "Godkjenn utkast til § 14 a-vedtak",
            description = """
                    Godkjenn det spesifiserte utkastet til § 14 a-vedtak. Følgende tilleggssteg vil utføres:
                    
                    * informasjon fra utkastet og om brukeren brukeren det er knyttet til vil bli fjernet fra kvalitetssikringsoversikten
                    * det blir produsert en systemmelding om at vedtaksutkastet er godkjent
                    * systemmeldingen postes i meldingskanalen mellom ansvarlig veileder og kvalitetssikrer
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content()),
                    @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public void godkjennVedtak(@RequestParam("vedtakId") long vedtakId) {
        // Sjekkar utrulling for kontoret til brukar ✅

		beslutterService.setGodkjentAvBeslutter(vedtakId);
	}

    @PutMapping("/status")
    @Operation(
            summary = "Oppdater status på kvalitetssikringen",
            description = """
                    Oppdaterer og setter ny status for kvalitetssikringen av utkast til § 14 a-vedtak:
                    
                    * dersom innlogget/autentisert veileder er ansvarlig veileder for utkastet vil ny status bli "Klar til kvalitetssikrer"
                    * dersom innlogget/autentisert veileder er kvalitetssikrer for utkastet vil ny status bli "Klar til ansvarlig veileder"
                    
                    Følgende tilleggssteg vil bli utført:
                    
                    * kvalitetssikringsoversikten vil bli oppdatert med informasjon om ny status
                    * det produseres en systemmelding om at vedtakutkastet har fått en ny status
                    * systemmeldingen postes i meldingskanalen mellom ansvarlig veileder og kvalitetssikrer
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
        // Sjekkar utrulling for kontoret til brukar ✅

		beslutterService.oppdaterBeslutterProsessStatus(vedtakId);
	}

}

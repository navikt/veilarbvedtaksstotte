package no.nav.veilarbvedtaksstotte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.service.UtrullingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/utrulling")
@Tag(
        name = "Utrulling",
        description = "Funksjonalitet knyttet til utrulling (hvilke kontorer som har tilgang til løsningen)."
)
public class UtrullingController {

    private final UtrullingService utrullingService;

    @Autowired
    public UtrullingController(UtrullingService utrullingService) {
        this.utrullingService = utrullingService;
    }

    @Deprecated(forRemoval = true)
    @GetMapping("/tilhorerBrukerUtrulletKontor")
    public boolean tilhorerBrukerUtrulletKontor(@RequestParam Fnr fnr) {
        return utrullingService.tilhorerBrukerUtrulletKontor(fnr);
    }

    @GetMapping("/tilhorerVeilederUtrulletKontor")
    @Operation(
            summary = "Tilhører veileder utrullet enhet",
            description = "Sjekker om innlogget/autentisert veileder tilhører (har tilgang til) minst en Nav-enhet hvor løsningen for § 14 a-vedtak er rullet ut.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = Boolean.class)
                            )
                    ),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true))),
            }
    )
    public boolean tilhorerVeilederUtrulletKontor() {
        return utrullingService.tilhorerInnloggetVeilederUtrulletKontor();
    }

    @GetMapping("/erUtrullet")
    @Operation(
            summary = "Er utrullet til enhet",
            description = "Sjekker om løsningen for § 14 a-vedtak er rullet ut til spesifisert Nav-enhet.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = Boolean.class)
                            )
                    ),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true))),
            }
    )
    public boolean erUtrullet(@RequestParam EnhetId enhetId) {
        return utrullingService.erUtrullet(enhetId);
    }

}

package no.nav.veilarbvedtaksstotte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.veilarbvedtaksstotte.controller.dto.KodeverkDTO;
import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalDetaljert;
import no.nav.veilarbvedtaksstotte.domain.vedtak.InnsatsgruppeDetaljert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/open/api/kodeverk")
@Tag(
        name = "Kodeverk",
        description = "Funksjonalitet knyttet til veilarbvedtaksstotte sitt kodeverk."
)
public class KodeverkController {
    KodeverkDTO kodeverk = new KodeverkDTO();

    @GetMapping("/innsatsgruppe")
    @Operation(
            summary = "Hent kodeverk for innsatsgruppe",
            description = "Henter kodeverket for innsatsgruppe.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = InnsatsgruppeDetaljert.class)))
                    )
            }
    )
    public InnsatsgruppeDetaljert[] getInnsatsgrupper() {
        return kodeverk.getInnsatsgrupper();
    }

    @GetMapping("/hovedmal")
    @Operation(
            summary = "Hent kodeverk for hovedmål",
            description = "Henter kodeverket for hovedmål.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = HovedmalDetaljert.class)))
                    )
            }
    )
    public HovedmalDetaljert[] getHovedmal() {
        return kodeverk.getHovedmal();
    }

    @GetMapping("/innsatsgruppeoghovedmal")
    @Operation(
            summary = "Hent kodeverk for innsatsgruppe og hovedmål",
            description = "Henter kodeverket for både innsatsgruppe og hovedmål.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = KodeverkDTO.class)))
                    )
            }
    )
    public KodeverkDTO getKodeverk() {
        return kodeverk;
    }
}

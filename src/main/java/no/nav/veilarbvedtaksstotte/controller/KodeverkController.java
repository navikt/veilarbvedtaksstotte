package no.nav.veilarbvedtaksstotte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.veilarbvedtaksstotte.annotations.EksterntEndepunkt;
import no.nav.veilarbvedtaksstotte.controller.dto.KodeverkDTO;
import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalDetaljert;
import no.nav.veilarbvedtaksstotte.domain.vedtak.InnsatsgruppeDetaljert;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/open/api/kodeverk")
@Tag(
        name = "Kodeverk for § 14 a-vedtak",
        description = "Funksjonalitet knyttet til kodeverk for data fra § 14 a-vedtak (innsatsgruppe og hovedmål)."
)
public class KodeverkController {
    KodeverkDTO kodeverk = new KodeverkDTO();

    @EksterntEndepunkt
    @GetMapping("/innsatsgruppe")
    @Operation(
            summary = "Hent kodeverk for innsatsgruppe",
            description = "Henter kodeverket for innsatsgrupper. En innsatsgruppe representerer konklusjonen i et § 14 a-vedtak. Med konklusjon menes hvilken/hvor mye arbeidsrettet oppfølging en bruker har rett på.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = InnsatsgruppeDetaljert.class))
                            )
                    )
            }
    )
    public InnsatsgruppeDetaljert[] getInnsatsgrupper() {
        return kodeverk.getInnsatsgrupper();
    }

    @EksterntEndepunkt
    @GetMapping("/hovedmal")
    @Operation(
            summary = "Hent kodeverk for hovedmål",
            description = "Henter kodeverket for hovedmål. Et hovedmål representerer brukers mål med den arbeidsrettede oppfølgingen og er en del av begrunnelsen i et § 14 a-vedtak.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = HovedmalDetaljert.class))
                            )
                    )
            }
    )
    public HovedmalDetaljert[] getHovedmal() {
        return kodeverk.getHovedmal();
    }

    @EksterntEndepunkt
    @GetMapping("/innsatsgruppeoghovedmal")
    @Operation(
            summary = "Hent kodeverk for innsatsgruppe og hovedmål",
            description = """
                    Henter kodeverket for både innsatsgruppe og hovedmål.
                    
                    En innsatsgruppe representerer konklusjonen i et § 14 a-vedtak. Med konklusjon menes hvilken/hvor mye arbeidsrettet oppfølging en bruker har rett på.
                    Et hovedmål representerer brukers mål med den arbeidsrettede oppfølgingen og er en del av begrunnelsen i et § 14 a-vedtak.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = KodeverkDTO.class)
                            )
                    )
            }
    )
    public KodeverkDTO getKodeverk() {
        return kodeverk;
    }
}

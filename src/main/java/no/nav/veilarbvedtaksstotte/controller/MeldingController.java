package no.nav.veilarbvedtaksstotte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.veilarbvedtaksstotte.controller.dto.DialogMeldingDTO;
import no.nav.veilarbvedtaksstotte.controller.dto.MeldingDTO;
import no.nav.veilarbvedtaksstotte.controller.dto.OpprettDialogMeldingDTO;
import no.nav.veilarbvedtaksstotte.controller.dto.SystemMeldingDTO;
import no.nav.veilarbvedtaksstotte.service.MeldingService;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/meldinger")
@Tag(
        name = "Meldinger",
        description = """
                Funksjonalitet knyttet til meldinger/meldingskanal mellom ansvarlig veileder og kvalitetssikrer. Alle meldinger er knyttet til et gitt utkast til § 14 a-vedtak. Meldinger kan enten:
                * være skrevet av ansvarlig veileder/kvalitetssikrer
                * være produsert av systemet (f.eks. i forbindelse med endring av status på kvalitetssikring)
                """
)
public class MeldingController {

    private final VedtakService vedtakService;

    private final MeldingService meldingService;

    @Autowired
    public MeldingController(VedtakService vedtakService, MeldingService meldingService) {
        this.vedtakService = vedtakService;
        this.meldingService = meldingService;
    }

    @GetMapping
    @Operation(
            summary = "Hent meldinger",
            description = "Henter alle meldinger mellom ansvarlig veileder og kvalitetssikrer som er knyttet til det " +
                    "spesifiserte utkastet til § 14 a-vedtak.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(
                                            schema = @Schema(
                                                    // Vi må spesifisere `type = "object"` her for at renderingen i Swagger UI skal bli riktig
                                                    // når vi bruker `anyOf`.
                                                    type = "object",
                                                    anyOf = {MeldingDTO.class, SystemMeldingDTO.class, DialogMeldingDTO.class}
                                            )
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public List<? extends MeldingDTO> hentDialogMeldinger(@RequestParam("vedtakId") long vedtakId) {
        if (vedtakService.erFattet(vedtakId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        return meldingService.hentMeldinger(vedtakId);
    }

    @PostMapping
    @Operation(
            summary = "Opprett melding",
            description = "Oppretter en ny melding og knytter den til det spesifiserte vedtaksutkastet. Meldingen vil " +
                    "bli synlig i meldingskanalen mellom ansvarlig veileder og kvalitetssikrer."
    )
    public void opprettDialogMelding(@RequestParam("vedtakId") long vedtakId, @RequestBody OpprettDialogMeldingDTO opprettDialogMeldingDTO) {
        if (vedtakService.erFattet(vedtakId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        meldingService.opprettBrukerDialogMelding(vedtakId, opprettDialogMeldingDTO.getMelding());
    }
}

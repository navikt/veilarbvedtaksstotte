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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/meldinger")
@Tag(
        name = "Meldinger",
        description = "Funksjonalitet knyttet til meldinger/meldingskanal mellom ansvarlig veileder og kvalitetssikrer. " +
                "Alle meldinger er knyttet til et gitt vedtaksutkast. Meldinger kan enten være produsert av systemet " +
                "(f.eks. i forbindelse med endring av status på kvalitetssikring) eller skrevet av ansvarlig veileder/kvalitetssikrer."
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
                    "spesifiserte vedtaksutkastet.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(array = @ArraySchema(schema = @Schema(oneOf = {MeldingDTO.class, DialogMeldingDTO.class, SystemMeldingDTO.class})))
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
            description = "Opprettet en ny melding og knytter den til det spesifiserte vedtaksutkastet. Meldingen vil " +
                    "bli synlig i meldingskanalen mellom ansvarlig veileder og kvalitetssikrer."
    )
    public void opprettDialogMelding(@RequestParam("vedtakId") long vedtakId, @RequestBody OpprettDialogMeldingDTO opprettDialogMeldingDTO) {
        if (vedtakService.erFattet(vedtakId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        meldingService.opprettBrukerDialogMelding(vedtakId, opprettDialogMeldingDTO.getMelding());
    }

}

package no.nav.veilarbvedtaksstotte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.controller.dto.BeslutterprosessStatusDTO;
import no.nav.veilarbvedtaksstotte.controller.dto.LagUtkastDTO;
import no.nav.veilarbvedtaksstotte.controller.dto.OppdaterUtkastDTO;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/utkast")
@Tag(
        name = "Utkast til § 14 a-vedtak",
        description = "Funksjonalitet knyttet til utkast til § 14 a-vedtak."
)
public class UtkastController {

    private final VedtakService vedtakService;

    @Autowired
    public UtkastController(VedtakService vedtakService) {
        this.vedtakService = vedtakService;
    }

    @Deprecated(forRemoval = true)
    @GetMapping
    public Vedtak hentUtkast(@RequestParam("fnr") Fnr fnr) {
        return vedtakService.hentUtkast(fnr);
    }

    @GetMapping("/{vedtakId}/beslutterprosessStatus")
    @Operation(
            summary = "Hent status på beslutterprosessen for det spesifiserte utkastet til § 14 a-vedtak.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BeslutterprosessStatusDTO.class))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public BeslutterprosessStatusDTO beslutterprosessStatus(
            @PathVariable("vedtakId") @Parameter(description = "ID-en til et utkast til § 14 a-vedtak") long vedtakId
    ) {
        return new BeslutterprosessStatusDTO(vedtakService.hentBeslutterprosessStatus(vedtakId));
    }

    @PostMapping
    @Operation(
            summary = "Opprett utkast til § 14 a-vedtak",
            description = "Oppretter et nytt utkast til § 14 a-vedtak knyttet til den spesifiserte brukeren.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema())),
                    @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public void lagUtkast(@RequestBody LagUtkastDTO lagUtkastDTO) {
        if (lagUtkastDTO == null || lagUtkastDTO.getFnr() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fnr");
        }
        vedtakService.lagUtkast(lagUtkastDTO.getFnr());
    }


    @PostMapping("/{vedtakId}/fattVedtak")
    @Operation(
            summary = "Fatt et § 14 a-vedtak",
            description = """
            Fatter et § 14 a-vedtak. Dette innebærer at:
            
            * det spesifiserte utkastet til § 14 a-vedtak låses for endringer
            * det genereres et PDF-dokument som representerer vedtaket og innholdet
            * det genereres PDF-dokumenter for tilleggsinformasjon, som f.eks. øybelikksbildet
            * PDF-dokumentene (vedtaket og tilleggsinformasjonen) journalføres og arkiveres
            * brev om vedtaket sendes til bruker i brukers foretrukne kanal (digitalt eller fysisk)
            """,
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema())),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public void fattVedtak(
            @PathVariable("vedtakId") @Parameter(description = "ID-en til et utkast til § 14 a-vedtak") long vedtakId
    ) {
        vedtakService.fattVedtak(vedtakId);
    }

    @PutMapping("/{vedtakId}")
    @Operation(
            summary = "Oppdater utkastet til § 14 a-vedtak",
            description = "Oppdaterer et utkast til § 14 a-vedtak med ny informasjon.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema())),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public void oppdaterUtkast(
            @PathVariable("vedtakId") @Parameter(description = "ID-en til et utkast til § 14 a-vedtak") long vedtakId,
            @RequestBody OppdaterUtkastDTO vedtakDTO
    ) {
        vedtakService.oppdaterUtkast(vedtakId, vedtakDTO);
    }

    // Brukes av veilarbvisittkfortfs (Skal fjernes)
    @Deprecated(forRemoval = true)
    @GetMapping("{fnr}/harUtkast")
    public boolean harUtkast(@PathVariable("fnr") Fnr fnr) {
        return vedtakService.harUtkast(fnr);
    }

    @GetMapping(value = "/{vedtakId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(
            summary = "Hent forhåndsvisning av et utkast til § 14 a-vedtak",
            description = "Genererer og returnerer et PDF-dokument for et gitt utkast til § 14 a-vedtak. PDF-dokumentet er en forhåndsvisning av PDF-dokumentet som eventuelt vil journalføres/arkiveres og sendes til bruker når vedtaket fattes.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public ResponseEntity<byte[]> hentForhandsvisning(
            @PathVariable("vedtakId") @Parameter(description = "ID-en til et utkast til § 14 a-vedtak") long vedtakId
    ) {
        byte[] utkastPdf = vedtakService.produserDokumentUtkast(vedtakId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "filename=vedtaksbrev-utkast.pdf")
                .body(utkastPdf);
    }

    @DeleteMapping("/{vedtakId}")
    @Operation(
            summary = "Slett utkast til § 14 a-vedtak",
            description = "Sletter det spesifiserte utkastet til § 14 a-vedtak.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema())),
                    @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public void deleteUtkast(
            @PathVariable("vedtakId") @Parameter(description = "ID-en til et utkast til § 14 a-vedtak") long vedtakId
    ) {
        vedtakService.slettUtkastSomVeileder(vedtakId);
    }

    @PostMapping("/{vedtakId}/overta")
    @Operation(
            summary = "Ta over som ansvarlig veileder",
            description = "Innlogget/autentisert veileder tar over som ansvarlig veileder for det spesifiserte utkastet til § 14 a-vedtak.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema())),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    public void oppdaterUtkast(
            @PathVariable("vedtakId") @Parameter(description = "ID-en til et utkast til § 14 a-vedtak") long vedtakId
    ) {
        vedtakService.taOverUtkast(vedtakId);
    }
}

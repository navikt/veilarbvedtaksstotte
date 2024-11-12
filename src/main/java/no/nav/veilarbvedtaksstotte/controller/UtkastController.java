package no.nav.veilarbvedtaksstotte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
        name = "Vedtaksutkast",
        description = "Funksjonalitet knyttet til vedtaksutkast."
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
            summary = "Hent status på beslutterprosessen for det spesifiserte vedtaksutkastet.",
            responses = {
                    @ApiResponse(responseCode = "403"),
                    @ApiResponse(responseCode = "404"),
                    @ApiResponse(responseCode = "500")
            }
    )
    public BeslutterprosessStatusDTO beslutterprosessStatus(
            @PathVariable("vedtakId") @Parameter(description = "ID-en til et vedtaksutkast") long vedtakId
    ) {
        return new BeslutterprosessStatusDTO(vedtakService.hentBeslutterprosessStatus(vedtakId));
    }

    @PostMapping
    @Operation(
            summary = "Opprett vedtaksutkast",
            description = "Oppretter et nytt vedtaksutkast knyttet til den spesifiserte brukeren.",
            responses = {
                    @ApiResponse(responseCode = "400"),
                    @ApiResponse(responseCode = "403"),
                    @ApiResponse(responseCode = "500")
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
            summary = "Fatt et vedtak",
            description = "Fatter et vedtak ved at det spesifiserte vedtaksutkastet låses for endringer samt at det journalføres/arkiveres og sendes til bruker.",
            responses = {
                    @ApiResponse(responseCode = "403"),
                    @ApiResponse(responseCode = "404"),
                    @ApiResponse(responseCode = "500")
            }
    )
    public void fattVedtak(
            @PathVariable("vedtakId") @Parameter(description = "ID-en til et vedtaksutkast") long vedtakId
    ) {
        vedtakService.fattVedtak(vedtakId);
    }

    @PutMapping("/{vedtakId}")
    @Operation(
            summary = "Oppdater vedtaksutkast",
            description = "Oppdaterer et vedtaksutkast med ny informasjon.",
            responses = {
                    @ApiResponse(responseCode = "403"),
                    @ApiResponse(responseCode = "404"),
                    @ApiResponse(responseCode = "500")
            }
    )
    public void oppdaterUtkast(
            @PathVariable("vedtakId") @Parameter(description = "ID-en til et vedtaksutkast") long vedtakId,
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
            summary = "Hent forhåndsvisning av vedtaksutkast",
            description = "Genererer og returnerer et PDF-dokument for et gitt vedtaksutkast. PDF-dokumentet er en forhåndsvisning av PDF-dokumentet som eventuelt vil journalføres/arkiveres og sendes til bruker når vedtaket fattes.",
            responses = {
                    @ApiResponse(responseCode = "403"),
                    @ApiResponse(responseCode = "404"),
                    @ApiResponse(responseCode = "500")
            }
    )
    public ResponseEntity<byte[]> hentForhandsvisning(
            @PathVariable("vedtakId") @Parameter(description = "ID-en til et vedtaksutkast") long vedtakId
    ) {
        byte[] utkastPdf = vedtakService.produserDokumentUtkast(vedtakId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "filename=vedtaksbrev-utkast.pdf")
                .body(utkastPdf);
    }

    @DeleteMapping("/{vedtakId}")
    @Operation(
            summary = "Slett vedtaksutkast",
            description = "Sletter et vedtaksutkast.",
            responses = {
                    @ApiResponse(responseCode = "400"),
                    @ApiResponse(responseCode = "403"),
                    @ApiResponse(responseCode = "404"),
                    @ApiResponse(responseCode = "500")
            }
    )
    public void deleteUtkast(
            @PathVariable("vedtakId") @Parameter(description = "ID-en til et vedtaksutkast") long vedtakId
    ) {
        vedtakService.slettUtkastSomVeileder(vedtakId);
    }

    @PostMapping("/{vedtakId}/overta")
    @Operation(
            summary = "Ta over som ansvarlig veileder",
            description = "Innlogget/autentisert veileder tar over som ansvarlig veileder for det spesifiserte vedtaksutkastet.",
            responses = {
                    @ApiResponse(responseCode = "403"),
                    @ApiResponse(responseCode = "404"),
                    @ApiResponse(responseCode = "500")
            }
    )
    public void oppdaterUtkast(
            @PathVariable("vedtakId") @Parameter(description = "ID-en til et vedtaksutkast") long vedtakId
    ) {
        vedtakService.taOverUtkast(vedtakId);
    }
}

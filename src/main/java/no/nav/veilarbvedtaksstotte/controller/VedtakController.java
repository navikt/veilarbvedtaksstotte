package no.nav.veilarbvedtaksstotte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.domain.arkiv.ArkivertVedtak;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.*;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.service.ArenaVedtakService;
import no.nav.veilarbvedtaksstotte.service.OyeblikksbildeService;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vedtak")
@Tag(
        name = "Fattede § 14 a-vedtak",
        description = """
                Funksjonalitet knyttet til fattede § 14 a-vedtak.
                
                Kommentar om øyeblikksbilde: begrepet "øyeblikksbilde" blir her brukt om opplysninger fra tidspunktet når § 14 a-vedtaket ble fattet og som har blitt journalført/arkivert sammen med selve vedtaket.
                """
)
public class VedtakController {

    private final VedtakService vedtakService;
    private final ArenaVedtakService arenaVedtakService;
    private final OyeblikksbildeService oyeblikksbildeService;
    private final AuditlogService auditlogService;

    @Autowired
    public VedtakController(VedtakService vedtakService, ArenaVedtakService arenaVedtakService, OyeblikksbildeService oyeblikksbildeService, AuditlogService auditlogService) {
        this.vedtakService = vedtakService;
        this.arenaVedtakService = arenaVedtakService;
        this.oyeblikksbildeService = oyeblikksbildeService;
        this.auditlogService = auditlogService;
    }

    @GetMapping(value = "{vedtakId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(
            summary = "Hent fattet § 14 a-vedtak",
            description = "Henter det spesifiserte fattede § 14 a-vedtaket i dokumentformat. Per dags dato støttes kun PDF-dokument (application/pdf).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PDF_VALUE,
                                    schema = @Schema(type = "string", format = "binary")
                            )
                    ),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true))),
            }
    )
    public ResponseEntity<byte[]> hentVedtakPdf(@PathVariable("vedtakId") long vedtakId) {
        byte[] vedtakPdf = vedtakService.hentVedtakPdf(vedtakId);
        auditlogService.auditlog(
                "Nav-ansatt hentet pdfversjon av fattet § 14 a-vedtak",
                auditlogService.finnFodselsnummerFraVedtakId(vedtakId)
        );
        return ResponseEntity.ok()
                .header("Content-Disposition", "filename=vedtaksbrev.pdf")
                .body(vedtakPdf);
    }

    @GetMapping(value = "{vedtakId}/{oyeblikksbildeType}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(
            summary = "Hent øyeblikksbilde",
            description = "Henter en gitt type øyeblikksbilde knyttet til det spesifiserte § 14 a-vedtaket på dokumentformat. Per dags dato støttes kun PDF-dokument (application/pdf).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PDF_VALUE,
                                    schema = @Schema(type = "string", format = "binary")
                            )
                    ),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true))),
            }
    )
    public ResponseEntity<byte[]> hentVedtakOyeblikksCVPdf(@PathVariable("vedtakId") long vedtakId, @PathVariable("oyeblikksbildeType") String oyeblikksbildeInputType) {
        auditlogService.auditlog(
                "Nav-ansatt hentet pdfversjon av kildende til et fattet § 14 a-vedtak",
                auditlogService.finnFodselsnummerFraVedtakId(vedtakId)
        );
        OyeblikksbildeType oyeblikksbildeType = OyeblikksbildeType.valueOf(oyeblikksbildeInputType);
        String dokumentId = oyeblikksbildeService.hentJournalfortDokumentId(vedtakId, oyeblikksbildeType);

        if (dokumentId == null) {
            return ResponseEntity.noContent().build();
        }

        byte[] oyeblikksbildePdf = vedtakService.hentOyeblikksbildePdf(vedtakId, dokumentId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "filename=vedtaksbrev.pdf")
                .body(oyeblikksbildePdf);
    }

    @Deprecated(forRemoval = true)
    @GetMapping("/fattet")
    public List<Vedtak> hentFattedeVedtak(@RequestParam("fnr") Fnr fnr) {
        auditlogService.auditlog(
                "Nav-ansatt hentet alle fattede § 14 a-vedtak på person",
                fnr
        );
        return vedtakService.hentFattedeVedtak(fnr);
    }


    @GetMapping("{vedtakId}/oyeblikksbilde-cv")
    @Operation(
            summary = "Hent CV-øyeblikksbilde",
            description = "Henter CV-opplysninger som ble journalført/arkivert sammen med det spesifiserte § 14 a-vedtaket " +
                    "på tidspunktet når det ble fattet.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = OyeblikksbildeCvDto.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
            }
    )
    public OyeblikksbildeCvDto hentCVOyeblikksbilde(@PathVariable("vedtakId") long vedtakId) {
        auditlogService.auditlog(
                "Nav-ansatt hentet kilden CV for et fattet § 14 a-vedtak",
                auditlogService.finnFodselsnummerFraVedtakId(vedtakId)
        );
        return oyeblikksbildeService.hentCVOyeblikksbildeForVedtak(vedtakId);
    }

    @GetMapping("{vedtakId}/oyeblikksbilde-registrering")
    @Operation(
            summary = "Hent arbeidssøkerregistrering-øyeblikksbilde (gammelt register)",
            description = "Henter arbeidssøker-opplysninger (fra gammelt register) som ble journalført/arkivert sammen " +
                    "med det spesifiserte § 14 a-vedtaket på tidspunktet når det ble fattet.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = OyeblikksbildeRegistreringDto.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
            }
    )
    public OyeblikksbildeRegistreringDto hentRegistreringOyeblikksbilde(@PathVariable("vedtakId") long vedtakId) {
        auditlogService.auditlog(
                "Nav-ansatt hentet kilden registreringsøyeblikksbilde for et fattet § 14 a-vedtak",
                auditlogService.finnFodselsnummerFraVedtakId(vedtakId)
        );
        return oyeblikksbildeService.hentRegistreringOyeblikksbildeForVedtak(vedtakId);
    }

    @GetMapping("{vedtakId}/oyeblikksbilde-arbeidssokerRegistret")
    @Operation(
            summary = "Hent arbeidssøkerregistrering-øyeblikksbilde (nytt register)",
            description = "Henter arbeidssøker-opplysninger (fra nytt register) som ble journalført/arkivert sammen med vedtaket på tidspunktet når " +
                    "det spesifiserte § 14 a-vedtaket ble fattet.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = OyeblikksbildeArbeidssokerRegistretDto.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
            }
    )
    public OyeblikksbildeArbeidssokerRegistretDto hentArbeidssokerRegistretOyeblikksbilde(@PathVariable("vedtakId") long vedtakId) {
        auditlogService.auditlog(
                "Nav-ansatt hentet kilden arbeidssøkerregistreringsøyeblikksbilde for et fattet § 14 a-vedtak",
                auditlogService.finnFodselsnummerFraVedtakId(vedtakId)
        );
        return oyeblikksbildeService.hentArbeidssokerRegistretOyeblikksbildeForVedtak(vedtakId);
    }

    @GetMapping("{vedtakId}/oyeblikksbilde-egenvurdering")
    @Operation(
            summary = "Hent egenvurdering-øyeblikksbilde (nytt register)",
            description = "Henter egenvurdering/behovsvurdering personen selv har gjort som ble journalført/arkivert sammen med vedtaket på tidspunktet når " +
                    "det spesifiserte § 14 a-vedtaket ble fattet.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(
                                    schema = @Schema(implementation = OyeblikksbildeArbeidssokerRegistretDto.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(hidden = true))),
            }
    )
    public OyeblikksbildeEgenvurderingDto hentEgenvurderingOyeblikksbilde(@PathVariable("vedtakId") long vedtakId) {
        auditlogService.auditlog(
                "Nav-ansatt hentet kilden egenvurdering/behovsvurdering-øyeblikksbilde for et fattet § 14 a-vedtak",
                auditlogService.finnFodselsnummerFraVedtakId(vedtakId)
        );
        return oyeblikksbildeService.hentEgenvurderingOyeblikksbildeForVedtak(vedtakId);
    }

    @Deprecated(forRemoval = true)
    @GetMapping("/arena")
    public List<ArkivertVedtak> hentVedtakFraArena(@RequestParam("fnr") Fnr fnr) {
        return arenaVedtakService.hentVedtakFraArena(fnr);
    }

    @GetMapping(value = "/arena/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(
            summary = "Hent fattet § 14 a-vedtak (Arena)",
            description = "Henter det spesifiserte fattede § 14 a-vedtaket i dokumentformat. Per dags dato støttes kun PDF-dokument (application/pdf).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PDF_VALUE,
                                    schema = @Schema(type = "string", format = "binary")
                            )
                    ),
                    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(hidden = true))),
            }
    )
    public ResponseEntity<byte[]> hentVedtakPdfFraArena(
            @RequestParam("dokumentInfoId") String dokumentInfoId,
            @RequestParam("journalpostId") String journalpostId) {
        byte[] vedtakPdf = arenaVedtakService.hentVedtakPdf(dokumentInfoId, journalpostId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "filename=vedtaksbrev.pdf")
                .body(vedtakPdf);
    }
}


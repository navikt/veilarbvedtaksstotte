package no.nav.veilarbvedtaksstotte.controller;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.domain.arkiv.ArkivertVedtak;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.Oyeblikksbilde;
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
public class VedtakController {

    private final VedtakService vedtakService;
    private final ArenaVedtakService arenaVedtakService;
    private final OyeblikksbildeService oyeblikksbildeService;

    @Autowired
    public VedtakController(VedtakService vedtakService, ArenaVedtakService arenaVedtakService, OyeblikksbildeService oyeblikksbildeService) {
        this.vedtakService = vedtakService;
        this.arenaVedtakService = arenaVedtakService;
        this.oyeblikksbildeService = oyeblikksbildeService;
    }

    @GetMapping(value = "{vedtakId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> hentVedtakPdf(@PathVariable("vedtakId") long vedtakId) {
        byte[] vedtakPdf = vedtakService.hentVedtakPdf(vedtakId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "filename=vedtaksbrev.pdf")
                .body(vedtakPdf);
    }

    @Deprecated(forRemoval = true)
    @GetMapping("/fattet")
    public List<Vedtak> hentFattedeVedtak(@RequestParam("fnr") Fnr fnr) {
        return vedtakService.hentFattedeVedtak(fnr);
    }

    @GetMapping("{vedtakId}/oyeblikksbilde")
    public List<Oyeblikksbilde> hentOyeblikksbilde(@PathVariable("vedtakId") long vedtakId) {
        return oyeblikksbildeService.hentOyeblikksbildeForVedtak(vedtakId);
    }

    @Deprecated(forRemoval = true)
    @GetMapping("/arena")
    public List<ArkivertVedtak> hentVedtakFraArena(@RequestParam("fnr") Fnr fnr) {
        return arenaVedtakService.hentVedtakFraArena(fnr);
    }

    @GetMapping(value = "/arena/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> hentVedtakPdfFraArena(
            @RequestParam("dokumentInfoId") String dokumentInfoId,
            @RequestParam("journalpostId") String journalpostId) {
        byte[] vedtakPdf = arenaVedtakService.hentVedtakPdf(dokumentInfoId, journalpostId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "filename=vedtaksbrev.pdf")
                .body(vedtakPdf);
    }
}


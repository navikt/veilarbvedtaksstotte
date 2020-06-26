package no.nav.veilarbvedtaksstotte.controller;

import no.nav.veilarbvedtaksstotte.domain.ArkivertVedtak;
import no.nav.veilarbvedtaksstotte.domain.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
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

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> hentVedtakPdf(@RequestParam("dokumentInfoId") String dokumentInfoId, @RequestParam("journalpostId") String journalpostId) {
        byte[] vedtakPdf = vedtakService.hentVedtakPdf(dokumentInfoId, journalpostId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "filename=vedtaksbrev.pdf")
                .body(vedtakPdf);
    }

    @GetMapping("/fattet")
    public List<Vedtak> hentFattedeVedtak(@RequestParam("fnr") String fnr) {
        return vedtakService.hentFattedeVedtak(fnr);
    }

    @GetMapping("/arena")
    public List<ArkivertVedtak> hentVedtakFraArena(@RequestParam("fnr") String fnr) {
        return arenaVedtakService.hentVedtakFraArena(fnr);
    }

    @GetMapping("/oyeblikksbilde/{vedtakId}")
    public List<Oyeblikksbilde> hentOyeblikksbilde(@PathVariable("vedtakId") long vedtakId) {
        return oyeblikksbildeService.hentOyeblikksbildeForVedtak(vedtakId);
    }

}


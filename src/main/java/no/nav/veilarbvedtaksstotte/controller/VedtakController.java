package no.nav.veilarbvedtaksstotte.controller;

import no.nav.veilarbvedtaksstotte.domain.*;
import no.nav.veilarbvedtaksstotte.service.ArenaVedtakService;
import no.nav.veilarbvedtaksstotte.service.OyeblikksbildeService;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.Response;
import java.util.List;

@RestController
@RequestMapping("/api/{fnr}")
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

    @PostMapping("/vedtak/send")
    public DokumentSendtDTO sendVedtak(@PathVariable("fnr") String fnr) {
        return vedtakService.sendVedtak(fnr);
    }

    @GetMapping(value = "/vedtak/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public Response hentVedtakPdf(@PathVariable("fnr") String fnr,
                                  @RequestParam("dokumentInfoId") String dokumentInfoId,
                                  @RequestParam("journalpostId") String journalpostId) {
        byte[] vedtakPdf = vedtakService.hentVedtakPdf(fnr, dokumentInfoId, journalpostId);
        return Response.ok(vedtakPdf)
                .header("Content-Disposition",  "filename=vedtaksbrev.pdf")
                .build();
    }

    @GetMapping("/vedtak")
    public List<Vedtak> hentVedtak(@PathVariable("fnr") String fnr) {
        return vedtakService.hentVedtak(fnr);
    }

    @GetMapping("/vedtakFraArena")
    public List<ArkivertVedtak> hentVedtakFraArena(@PathVariable("fnr") String fnr) {
        return arenaVedtakService.hentVedtakFraArena(fnr);
    }

    @PostMapping("/utkast")
    public void lagUtkast(@PathVariable("fnr") String fnr) {
        vedtakService.lagUtkast(fnr);
    }

    @PutMapping("/utkast")
    public void oppdaterUtkast(@PathVariable("fnr") String fnr, @RequestBody VedtakDTO vedtakDTO) {
        vedtakService.oppdaterUtkast(fnr, vedtakDTO);
    }

    @GetMapping("/harUtkast")
    public boolean harUtkast(@PathVariable("fnr") String fnr) {
       return vedtakService.harUtkast(fnr);
    }

    @GetMapping(value = "/utkast/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public Response hentForhandsvisning(@PathVariable("fnr") String fnr) {
        byte[] utkastPdf = vedtakService.produserDokumentUtkast(fnr);
        return Response.ok(utkastPdf)
                .header("Content-Disposition", "filename=vedtaksbrev-utkast.pdf")
                .build();
    }

    @DeleteMapping("/utkast")
    public void deleteUtkast(@PathVariable("fnr") String fnr) { vedtakService.slettUtkastForFnr(fnr); }

    @GetMapping("/oyeblikksbilde/{vedtakid}")
    public List<Oyeblikksbilde> hentOyeblikksbilde(@PathVariable("fnr") String fnr, @PathVariable("vedtakid") long vedtakId) {
        return oyeblikksbildeService.hentOyeblikksbildeForVedtak(fnr, vedtakId);
    }

    @PostMapping("/utkast/overta")
    public void oppdaterUtkast(@PathVariable("fnr") String fnr) {
        vedtakService.taOverUtkast(fnr);
    }
}


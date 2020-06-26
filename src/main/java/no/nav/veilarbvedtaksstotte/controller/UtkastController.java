package no.nav.veilarbvedtaksstotte.controller;

import no.nav.veilarbvedtaksstotte.domain.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.domain.LagUtkastDTO;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.VedtakDTO;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/utkast")
public class UtkastController {

    private final VedtakService vedtakService;

    @Autowired
    public UtkastController(VedtakService vedtakService) {
        this.vedtakService = vedtakService;
    }

    @GetMapping
    public Vedtak hentUtkast(@RequestParam("fnr") String fnr) {
        return vedtakService.hentUtkast(fnr);
    }

    @PostMapping
    public void lagUtkast(LagUtkastDTO lagUtkastDTO) {
        vedtakService.lagUtkast(lagUtkastDTO.getFnr());
    }

    @PostMapping("/{vedtakId}/fattVedtak")
    public DokumentSendtDTO fattVedtak(@PathVariable("vedtakId") long vedtakId) {
        return vedtakService.fattVedtak(vedtakId);
    }

    @PutMapping("/{vedtakId}")
    public void oppdaterUtkast(@PathVariable("vedtakId") long vedtakId, @RequestBody VedtakDTO vedtakDTO) {
        vedtakService.oppdaterUtkast(vedtakId, vedtakDTO);
    }

    // Brukes av veilarbvisittkfortfs (Skal fjernes)
    @GetMapping("{fnr}/harUtkast")
    public boolean harUtkast(@PathVariable("fnr") String fnr) {
        return vedtakService.harUtkast(fnr);
    }

    @GetMapping(value = "/{vedtakId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> hentForhandsvisning(@PathVariable("vedtakId") long vedtakId) {
        byte[] utkastPdf = vedtakService.produserDokumentUtkast(vedtakId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "filename=vedtaksbrev-utkast.pdf")
                .body(utkastPdf);
    }

    @DeleteMapping("/{vedtakId}")
    public void deleteUtkast(@PathVariable("vedtakId") long vedtakId) { vedtakService.slettUtkast(vedtakId); }

    @PostMapping("/{vedtakId}/overta")
    public void oppdaterUtkast(@PathVariable("vedtakId") long vedtakId) {
        vedtakService.taOverUtkast(vedtakId);
    }

}

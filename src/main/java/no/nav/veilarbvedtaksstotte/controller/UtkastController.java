package no.nav.veilarbvedtaksstotte.controller;

import no.nav.veilarbvedtaksstotte.domain.*;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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


    @GetMapping("/{vedtakId}/erGodkjent")
    public ErGodkjentDTO erGodkjentAvBeslutter(@PathVariable("vedtakId") long vedtakId) {
        return new ErGodkjentDTO(vedtakService.erUtkastGodkjentAvBeslutter(vedtakId));
    }

    @PostMapping
    public void lagUtkast(@RequestBody LagUtkastDTO lagUtkastDTO) {
        if (lagUtkastDTO == null || lagUtkastDTO.getFnr() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fnr");
        }
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

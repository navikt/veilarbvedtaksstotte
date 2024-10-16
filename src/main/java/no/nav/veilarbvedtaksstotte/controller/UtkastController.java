package no.nav.veilarbvedtaksstotte.controller;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/utkast")
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
    public BeslutterprosessStatusDTO beslutterprosessStatus(@PathVariable("vedtakId") long vedtakId) {
        return new BeslutterprosessStatusDTO(vedtakService.hentBeslutterprosessStatus(vedtakId));
    }

    @PostMapping
    public void lagUtkast(@RequestBody LagUtkastDTO lagUtkastDTO) {
        if (lagUtkastDTO == null || lagUtkastDTO.getFnr() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fnr");
        }
        vedtakService.lagUtkast(lagUtkastDTO.getFnr());
    }

    @PostMapping("/{vedtakId}/fattVedtak")
    public void fattVedtak(@PathVariable("vedtakId") long vedtakId) {
        vedtakService.fattVedtak(vedtakId);
    }

    @PutMapping("/{vedtakId}")
    public void oppdaterUtkast(@PathVariable("vedtakId") long vedtakId, @RequestBody OppdaterUtkastDTO vedtakDTO) {
        vedtakService.oppdaterUtkast(vedtakId, vedtakDTO);
    }

    // Brukes av veilarbvisittkfortfs (Skal fjernes)
    @Deprecated(forRemoval = true)
    @GetMapping("{fnr}/harUtkast")
    public boolean harUtkast(@PathVariable("fnr") Fnr fnr) {
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
    public void deleteUtkast(@PathVariable("vedtakId") long vedtakId) { vedtakService.slettUtkastSomVeileder(vedtakId); }

    @PostMapping("/{vedtakId}/overta")
    public void oppdaterUtkast(@PathVariable("vedtakId") long vedtakId) {
        vedtakService.taOverUtkast(vedtakId);
    }

}

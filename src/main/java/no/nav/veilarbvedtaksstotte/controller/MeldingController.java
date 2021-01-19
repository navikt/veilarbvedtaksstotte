package no.nav.veilarbvedtaksstotte.controller;

import no.nav.veilarbvedtaksstotte.controller.dto.MeldingDTO;
import no.nav.veilarbvedtaksstotte.controller.dto.OpprettDialogMeldingDTO;
import no.nav.veilarbvedtaksstotte.service.MeldingService;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/meldinger")
public class MeldingController {

    private final VedtakService vedtakService;

    private final MeldingService meldingService;

    @Autowired
    public MeldingController(VedtakService vedtakService, MeldingService meldingService) {
        this.vedtakService = vedtakService;
        this.meldingService = meldingService;
    }

    @GetMapping
    public List<MeldingDTO> hentDialogMeldinger(@RequestParam("vedtakId") long vedtakId) {
        if (vedtakService.erFattet(vedtakId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        return meldingService.hentMeldinger(vedtakId);
    }

    @PostMapping
    public void opprettDialogMelding(@RequestParam("vedtakId") long vedtakId, @RequestBody OpprettDialogMeldingDTO opprettDialogMeldingDTO) {
        if (vedtakService.erFattet(vedtakId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        meldingService.opprettBrukerDialogMelding(vedtakId, opprettDialogMeldingDTO.getMelding());
    }

}

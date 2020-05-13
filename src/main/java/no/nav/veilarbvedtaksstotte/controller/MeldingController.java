package no.nav.veilarbvedtaksstotte.controller;

import no.nav.veilarbvedtaksstotte.domain.OpprettDialogMeldingDTO;
import no.nav.veilarbvedtaksstotte.domain.dialog.MeldingDTO;
import no.nav.veilarbvedtaksstotte.service.MeldingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{fnr}/meldinger")
public class MeldingController {

    private final MeldingService meldingService;

    @Autowired
    public MeldingController(MeldingService meldingService) {
        this.meldingService = meldingService;
    }

    @GetMapping
    public List<MeldingDTO> hentDialogMeldinger(@PathVariable("fnr") String fnr) {
        return meldingService.hentMeldinger(fnr);
    }

    @PostMapping
    public void opprettDialogMelding(@PathVariable("fnr") String fnr, @RequestBody OpprettDialogMeldingDTO opprettDialogMeldingDTO) {
        meldingService.opprettBrukerDialogMelding(fnr, opprettDialogMeldingDTO.getMelding());
    }

}

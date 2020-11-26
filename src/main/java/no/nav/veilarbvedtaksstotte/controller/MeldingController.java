package no.nav.veilarbvedtaksstotte.controller;

import no.nav.veilarbvedtaksstotte.controller.dto.OpprettDialogMeldingDTO;
import no.nav.veilarbvedtaksstotte.domain.dialog.MeldingDTO;
import no.nav.veilarbvedtaksstotte.service.MeldingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meldinger")
public class MeldingController {

    private final MeldingService meldingService;

    @Autowired
    public MeldingController(MeldingService meldingService) {
        this.meldingService = meldingService;
    }

    @GetMapping
    public List<MeldingDTO> hentDialogMeldinger(@RequestParam("vedtakId") long vedtakId) {
        return meldingService.hentMeldinger(vedtakId);
    }

    @PostMapping
    public void opprettDialogMelding(@RequestParam("vedtakId") long vedtakId, @RequestBody OpprettDialogMeldingDTO opprettDialogMeldingDTO) {
        meldingService.opprettBrukerDialogMelding(vedtakId, opprettDialogMeldingDTO.getMelding());
    }

}

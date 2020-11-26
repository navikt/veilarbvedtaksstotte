package no.nav.veilarbvedtaksstotte.controller;

import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BrukereMedAntall;
import no.nav.veilarbvedtaksstotte.service.BeslutteroversiktService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beslutteroversikt")
public class BeslutteroversiktController {

    private final BeslutteroversiktService beslutteroversiktService;

    @Autowired
    public BeslutteroversiktController(BeslutteroversiktService beslutteroversiktService) {
        this.beslutteroversiktService = beslutteroversiktService;
    }

    @PostMapping("/sok")
    public BrukereMedAntall startBeslutterProsess(@RequestBody BeslutteroversiktSok sokData) {
        return beslutteroversiktService.sokEtterBruker(sokData);
    }

}

package no.nav.veilarbvedtaksstotte.controller;

import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.domain.BrukereMedAntall;
import no.nav.veilarbvedtaksstotte.service.BeslutteroversiktService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beslutteroversikt")
public class BeslutteroversiktResource {

    private final BeslutteroversiktService beslutteroversiktService;

    @Autowired
    public BeslutteroversiktResource(BeslutteroversiktService beslutteroversiktService) {
        this.beslutteroversiktService = beslutteroversiktService;
    }

    @PostMapping("/sok")
    public BrukereMedAntall startBeslutterProsess(BeslutteroversiktSok sokData) {
        return beslutteroversiktService.sokEtterBruker(sokData);
    }

}

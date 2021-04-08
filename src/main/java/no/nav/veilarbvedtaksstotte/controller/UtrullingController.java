package no.nav.veilarbvedtaksstotte.controller;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.service.UtrullingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/utrulling")
public class UtrullingController {

    private final UtrullingService utrullingService;

    @Autowired
    public UtrullingController(UtrullingService utrullingService) {
        this.utrullingService = utrullingService;
    }

    @GetMapping("/tilhorerBrukerUtrulletKontor")
    public boolean tilhorerBrukerUtrulletKontor(@RequestParam Fnr fnr) {
        return utrullingService.tilhorerBrukerUtrulletKontor(fnr);
    }

    @GetMapping("/tilhorerVeilederUtrulletKontor")
    public boolean tilhorerVeilederUtrulletKontor() {
        return utrullingService.tilhorerInnloggetVeilederUtrulletKontor();
    }

    @GetMapping("/erUtrullet/{enhetId}")
    public boolean erUtrullet(@PathVariable EnhetId enhetId) {
        return utrullingService.erUtrullet(enhetId);
    }

}

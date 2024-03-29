package no.nav.veilarbvedtaksstotte.controller;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.service.UtrullingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/utrulling")
public class UtrullingController {

    private final UtrullingService utrullingService;

    @Autowired
    public UtrullingController(UtrullingService utrullingService) {
        this.utrullingService = utrullingService;
    }

    @Deprecated(forRemoval = true)
    @GetMapping("/tilhorerBrukerUtrulletKontor")
    public boolean tilhorerBrukerUtrulletKontor(@RequestParam Fnr fnr) {
        return utrullingService.tilhorerBrukerUtrulletKontor(fnr);
    }

    @GetMapping("/tilhorerVeilederUtrulletKontor")
    public boolean tilhorerVeilederUtrulletKontor() {
        return utrullingService.tilhorerInnloggetVeilederUtrulletKontor();
    }

    @GetMapping("/erUtrullet")
    public boolean erUtrullet(@RequestParam EnhetId enhetId) {
        return utrullingService.erUtrullet(enhetId);
    }

}

package no.nav.veilarbvedtaksstotte.controller;

import no.nav.common.types.identer.EnhetId;
import no.nav.veilarbvedtaksstotte.controller.dto.UtrulletDTO;
import no.nav.veilarbvedtaksstotte.service.UtrullingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/utrulling")
public class UtrullingController {

    private final UtrullingService utrullingService;

    @Autowired
    public UtrullingController(UtrullingService utrullingService) {
        this.utrullingService = utrullingService;
    }

    @GetMapping("/{enhetId}")
    public UtrulletDTO harTilgang(@PathVariable EnhetId enhetId) {
        return new UtrulletDTO(utrullingService.erUtrullet(enhetId));
    }

}

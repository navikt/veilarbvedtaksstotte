package no.nav.veilarbvedtaksstotte.controller;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.types.identer.EnhetId;
import no.nav.veilarbvedtaksstotte.repository.domain.UtrulletEnhet;
import no.nav.veilarbvedtaksstotte.service.UtrullingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    public final static String PTO_ADMIN_SERVICE_USER = "srvpto-admin-api";

    private final UtrullingService utrullingService;

    private final AuthContextHolder authContextHolder;

    @Autowired
    public AdminController(UtrullingService utrullingService, AuthContextHolder authContextHolder) {
        this.utrullingService = utrullingService;
        this.authContextHolder = authContextHolder;
    }

    @GetMapping("/utrulling")
    public List<UtrulletEnhet> hentAlleUtrullinger() {
        sjekkTilgangTilAdmin();
        return utrullingService.hentAlleUtrullinger();
    }

    @PostMapping("/utrulling/{enhetId}")
    public void leggTilUtrulling(@PathVariable EnhetId enhetId) {
        sjekkTilgangTilAdmin();
        utrullingService.leggTilUtrulling(enhetId);
    }

    @DeleteMapping("/utrulling/{enhetId}")
    public void fjernUtrulling(@PathVariable EnhetId enhetId) {
        sjekkTilgangTilAdmin();
        utrullingService.fjernUtrulling(enhetId);
    }

    private void sjekkTilgangTilAdmin() {
        String subject = authContextHolder.getSubject()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!PTO_ADMIN_SERVICE_USER.equals(subject)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

}

package no.nav.veilarbvedtaksstotte.controller;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.types.identer.EnhetId;
import no.nav.veilarbvedtaksstotte.repository.domain.EnhetTilgang;
import no.nav.veilarbvedtaksstotte.service.TilgangskontrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final static String PTO_ADMIN_SERVICE_USER = "srvpto-admin-api";

    private final TilgangskontrollService tilgangskontrollService;

    private final AuthContextHolder authContextHolder;

    @Autowired
    public AdminController(TilgangskontrollService tilgangskontrollService, AuthContextHolder authContextHolder) {
        this.tilgangskontrollService = tilgangskontrollService;
        this.authContextHolder = authContextHolder;
    }

    @GetMapping("/tilgang")
    public List<EnhetTilgang> hentAlleTilganger() {
        sjekkTilgangTilAdmin();
        return tilgangskontrollService.hentAlleTilganger();
    }

    @PostMapping("/tilgang/{enhetId}")
    public void leggTilTilgang(@PathVariable EnhetId enhetId) {
        sjekkTilgangTilAdmin();
        tilgangskontrollService.lagNyTilgang(enhetId);
    }

    @DeleteMapping("/tilgang/{enhetId}")
    public void fjernTilgang(@PathVariable EnhetId enhetId) {
        sjekkTilgangTilAdmin();
        tilgangskontrollService.fjernTilgang(enhetId);
    }

    private void sjekkTilgangTilAdmin() {
        String subject = authContextHolder.getSubject()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!PTO_ADMIN_SERVICE_USER.equals(subject)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

}

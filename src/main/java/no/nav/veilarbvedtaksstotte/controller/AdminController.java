package no.nav.veilarbvedtaksstotte.controller;

import no.nav.common.job.JobRunner;
import no.nav.common.types.identer.EnhetId;
import no.nav.veilarbvedtaksstotte.repository.domain.UtrulletEnhet;
import no.nav.veilarbvedtaksstotte.service.AuthService;
import no.nav.veilarbvedtaksstotte.service.KafkaRepubliseringService;
import no.nav.veilarbvedtaksstotte.service.UtrullingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    public final static String PTO_ADMIN = "pto-admin";

    private final UtrullingService utrullingService;

    private final AuthService authService;

    private final KafkaRepubliseringService kafkaRepubliseringService;

    @Autowired
    public AdminController(UtrullingService utrullingService,
                           AuthService authService,
                           KafkaRepubliseringService kafkaRepubliseringService) {
        this.utrullingService = utrullingService;
        this.authService = authService;
        this.kafkaRepubliseringService = kafkaRepubliseringService;
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

    @PostMapping("/republiser/siste-14a-vedtak")
    public String republiserSiste14aVedtak() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync(
                "republiser-siste-14a-vedtak",
                kafkaRepubliseringService::republiserSiste14aVedtak
        );
    }

    @PostMapping("/republiser/vedtak-14a-fattet-dvh")
    public String republiserVedtak14aFattetDvh() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync(
                "republiser-vedtak-14a-fattet-dvh",
                () -> kafkaRepubliseringService.republiserVedtak14aFattetDvh(100)
        );
    }

    private void sjekkTilgangTilAdmin() {
        boolean erSystemBrukerFraAzure = authService.erSystemBruker();
        boolean erPtoAdmin = PTO_ADMIN.equals(authService.hentApplikasjonFraContex());

        if (erPtoAdmin && erSystemBrukerFraAzure) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

}

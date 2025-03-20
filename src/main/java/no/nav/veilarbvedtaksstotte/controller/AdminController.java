package no.nav.veilarbvedtaksstotte.controller;

import jakarta.ws.rs.ForbiddenException;
import no.nav.common.job.JobRunner;
import no.nav.common.types.identer.EnhetId;
import no.nav.veilarbvedtaksstotte.repository.domain.UtrulletEnhet;
import no.nav.veilarbvedtaksstotte.service.AuthService;
import no.nav.veilarbvedtaksstotte.service.KafkaRepubliseringService;
import no.nav.veilarbvedtaksstotte.service.UtrullingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    public static final String POAO_ADMIN = "poao-admin";

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
        boolean erInternBruker = authService.erInternBruker();
        boolean erPtoAdmin = POAO_ADMIN.equals(authService.hentApplikasjonFraContex());

        if (erPtoAdmin && erInternBruker) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

}

package no.nav.veilarbvedtaksstotte.controller;

import no.nav.common.job.JobRunner;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbvedtaksstotte.controller.dto.SlettVedtakRequest;
import no.nav.veilarbvedtaksstotte.repository.domain.UtrulletEnhet;
import no.nav.veilarbvedtaksstotte.service.AuthService;
import no.nav.veilarbvedtaksstotte.service.KafkaRepubliseringService;
import no.nav.veilarbvedtaksstotte.service.UtrullingService;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    public static final String POAO_ADMIN = "poao-admin";

    private final UtrullingService utrullingService;

    private final AuthService authService;

    private final KafkaRepubliseringService kafkaRepubliseringService;

    private final VedtakService vedtakService;

    @Autowired
    public AdminController(UtrullingService utrullingService,
                           AuthService authService,
                           KafkaRepubliseringService kafkaRepubliseringService,
                           VedtakService vedtakService
    ) {
        this.utrullingService = utrullingService;
        this.authService = authService;
        this.kafkaRepubliseringService = kafkaRepubliseringService;
        this.vedtakService = vedtakService;
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

    /**
     * OBS: Denne slettingen skal kun brukes ved personvernsbrudd.
     */
    @PutMapping("/slett-vedtak")
    public void slettVedtak(@RequestBody SlettVedtakRequest slettVedtakRequest) {
        sjekkTilgangTilAdmin();
        if (!isDevelopment().orElse(false)) {
            authService.erInnloggetBrukerModiaAdmin();
        }
        String regex = "[A-Za-z]\\d{6}";

        if (!slettVedtakRequest.getAnsvarligVeileder().get().matches(regex)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ansvarlig veileder må ha formatet X123456");
        }
        vedtakService.slettVedtak(slettVedtakRequest, NavIdent.of(authService.getInnloggetVeilederIdent()));
    }

    private void sjekkTilgangTilAdmin() {
        boolean erInternBruker = authService.erInternBruker();
        boolean erPoaoAdmin = POAO_ADMIN.equals(authService.hentApplikasjonFraContex());

        if (erPoaoAdmin && erInternBruker) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

}

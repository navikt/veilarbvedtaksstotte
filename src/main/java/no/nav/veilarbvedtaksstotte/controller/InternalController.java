package no.nav.veilarbvedtaksstotte.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.selftest.SelfTestCheck;
import no.nav.common.health.selftest.SelfTestUtils;
import no.nav.common.health.selftest.SelftTestCheckResult;
import no.nav.common.health.selftest.SelftestHtmlGenerator;
import no.nav.veilarbvedtaksstotte.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

import static no.nav.common.health.selftest.SelfTestUtils.checkAllParallel;

@Slf4j
@RestController
@RequestMapping("/internal")
public class InternalController {

    private final JdbcTemplate db;

    private final List<SelfTestCheck> selftestChecks;

    @Autowired
    public InternalController(
            ArenaClient arenaClient, DokumentClient dokumentClient, EgenvurderingClient egenvurderingClient,
            OppfolgingClient oppfolgingClient, PamCvClient pamCvClient, PersonClient personClient,
            RegistreringClient registreringClient, SafClient safClient, VeiledereOgEnhetClient veiledereOgEnhetClient,
            JdbcTemplate db
    ) {
        this.db = db;

        this.selftestChecks = Arrays.asList(
                new SelfTestCheck("ArenaClient", false, arenaClient),
                new SelfTestCheck("DokumentClient", false, dokumentClient),
                new SelfTestCheck("EgenvurderingClient", false, egenvurderingClient),
                new SelfTestCheck("OppfolgingClient", false, oppfolgingClient),
                new SelfTestCheck("PamCvClient", false, pamCvClient),
                new SelfTestCheck("PersonClient", false, personClient),
                new SelfTestCheck("RegistreringClient", false, registreringClient),
                new SelfTestCheck("SafClient", false, safClient),
                new SelfTestCheck("VeilederOgEnhetClient", false, veiledereOgEnhetClient),
                new SelfTestCheck("Ping database", true, this::checkDbHealth)
        );
    }

    @GetMapping("/isReady")
    public void isReady() {
        isAlive();
    }

    @GetMapping("/isAlive")
    public void isAlive() {
        HealthCheckResult dbHealthCheckResult = checkDbHealth();
        if (!dbHealthCheckResult.isHealthy()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/selftest")
    public ResponseEntity selftest() {
        List<SelftTestCheckResult> checkResults = checkAllParallel(selftestChecks);
        String html = SelftestHtmlGenerator.generate(checkResults);
        int status = SelfTestUtils.findHttpStatusCode(checkResults, true);

        return ResponseEntity
                .status(status)
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private HealthCheckResult checkDbHealth() {
        try {
            db.query("SELECT 1", resultSet -> {});
            return HealthCheckResult.healthy();
        } catch (Exception e) {
            log.error("Helsesjekk mot database feilet", e);
            return HealthCheckResult.unhealthy("Fikk ikke kontakt med databasen", e);
        }
    }

}

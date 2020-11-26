package no.nav.veilarbvedtaksstotte.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.selftest.SelfTestCheck;
import no.nav.common.health.selftest.SelfTestUtils;
import no.nav.common.health.selftest.SelftTestCheckResult;
import no.nav.common.health.selftest.SelftestHtmlGenerator;
import no.nav.veilarbvedtaksstotte.client.arena.ArenaClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.dokument.DokumentClient;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.EgenvurderingClient;
import no.nav.veilarbvedtaksstotte.client.oppfolging.OppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.registrering.RegistreringClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeiledereOgEnhetClient;
import no.nav.veilarbvedtaksstotte.kafka.KafkaHelsesjekk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

import static no.nav.common.health.selftest.SelfTestUtils.checkAllParallel;

@Slf4j
@RestController
@RequestMapping("/internal")
public class InternalController {

    private final List<SelfTestCheck> selftestChecks;

    private final DataSourceHealthIndicator dataSourceHealthIndicator;

    @Autowired
    public InternalController(
            ArenaClient arenaClient, DokumentClient dokumentClient, EgenvurderingClient egenvurderingClient,
            OppfolgingClient oppfolgingClient, VeilarbpersonClient veilarbpersonClient,
            RegistreringClient registreringClient, SafClient safClient, VeiledereOgEnhetClient veiledereOgEnhetClient,
            KafkaHelsesjekk kafkaHelsesjekk, DataSourceHealthIndicator dataSourceHealthIndicator
    ) {
        this.dataSourceHealthIndicator = dataSourceHealthIndicator;

        this.selftestChecks = Arrays.asList(
                new SelfTestCheck("ArenaClient", false, arenaClient),
                new SelfTestCheck("DokumentClient", false, dokumentClient),
                new SelfTestCheck("EgenvurderingClient", false, egenvurderingClient),
                new SelfTestCheck("OppfolgingClient", false, oppfolgingClient),
                new SelfTestCheck("PersonClient", false, veilarbpersonClient),
                new SelfTestCheck("RegistreringClient", false, registreringClient),
                new SelfTestCheck("SafClient", false, safClient),
                new SelfTestCheck("VeilederOgEnhetClient", false, veiledereOgEnhetClient),
                new SelfTestCheck("Kafka Consumer", false, kafkaHelsesjekk),
                new SelfTestCheck("Ping database", true, this::checkDbHealth)
        );
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
        Health health = dataSourceHealthIndicator.health();
        if (Status.UP.equals(health.getStatus())) {
            return HealthCheckResult.healthy();
        } else {
            return HealthCheckResult.unhealthy("Fikk ikke kontakt med databasen" + health.getDetails().toString());
        }
    }

}

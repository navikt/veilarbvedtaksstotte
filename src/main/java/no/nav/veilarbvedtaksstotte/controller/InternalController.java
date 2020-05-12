package no.nav.veilarbvedtaksstotte.controller;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthChecker;
import no.nav.common.health.selftest.SelfTestCheck;
import no.nav.common.health.selftest.SelfTestStatus;
import no.nav.common.health.selftest.SelftTestCheckResult;
import no.nav.common.health.selftest.SelftestHtmlGenerator;
import no.nav.common.utils.EnvironmentUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static no.nav.common.health.selftest.SelfTestUtils.aggregateStatus;
import static no.nav.common.health.selftest.SelfTestUtils.checkAll;

@RestController
@RequestMapping("/internal")
public class InternalController {

    private final static HealthCheck check1 = HealthCheckResult::healthy;

    private final static HealthCheck check2 = HealthCheckResult::healthy;

    private final static List<HealthCheck> isAliveChecks = List.of(check1);

    private final static List<HealthCheck> isReadyChecks = List.of(check1, check2);

    private final static List<SelfTestCheck> selftestChecks = List.of(
            new SelfTestCheck("Check 1", false, check1),
            new SelfTestCheck("Check 2", false, check2)
    );

    //    @Bean
//    public Pingable dbPinger(JdbcTemplate db) {
//        HelsesjekkMetadata metadata = new HelsesjekkMetadata("db",
//                "Database: " + getRequiredProperty(VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY),
//                "Database for veilarbvedtaksstotte",
//                true);
//
//        return () -> {
//            try {
//                db.execute("SELECT 1");
//                return Pingable.Ping.lyktes(metadata);
//            } catch (Exception e) {
//                return Pingable.Ping.feilet(metadata, e);
//            }
//        };
//    }

    @GetMapping("/isReady")
    public void isReady() {
        if (HealthChecker.findFirstFailingCheck(isReadyChecks).isPresent()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/isAlive")
    public void isAlive() {
        if (HealthChecker.findFirstFailingCheck(isAliveChecks).isPresent()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/selftest")
    public ResponseEntity selftest() {
        List<SelftTestCheckResult> checkResults = checkAll(selftestChecks);
        String html = SelftestHtmlGenerator.generate(checkResults, EnvironmentUtils.resolveHostName(), LocalDateTime.now());
        int status = aggregateStatus(checkResults) == SelfTestStatus.ERROR ? 500 : 200;
        return ResponseEntity.status(status).body(html);
    }

}

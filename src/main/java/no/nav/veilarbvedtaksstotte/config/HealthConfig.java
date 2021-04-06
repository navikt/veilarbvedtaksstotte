package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.selftest.SelfTestCheck;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestMeterBinder;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.dokument.VeilarbdokumentClient;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.VeilarbvedtakinfoClient;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

@Configuration
public class HealthConfig {

    @Bean
    public DataSourceHealthIndicator dataSourceHealthIndicator(DataSource dataSource) {
        return new DataSourceHealthIndicator(dataSource, "SELECT 1");
    }

    @Bean
    public SelfTestChecks selfTestChecks(VeilarbarenaClient arenaClient,
                                         VeilarbdokumentClient dokumentClient,
                                         VeilarbvedtakinfoClient egenvurderingClient,
                                         VeilarboppfolgingClient oppfolgingClient,
                                         VeilarbpersonClient veilarbpersonClient,
                                         VeilarbregistreringClient registreringClient,
                                         SafClient safClient,
                                         VeilarbveilederClient veiledereOgEnhetClient,
                                         DataSourceHealthIndicator dataSourceHealthIndicator) {

        List<SelfTestCheck> selfTestChecks = Arrays.asList(
                new SelfTestCheck("ArenaClient", false, arenaClient),
                new SelfTestCheck("DokumentClient", false, dokumentClient),
                new SelfTestCheck("EgenvurderingClient", false, egenvurderingClient),
                new SelfTestCheck("OppfolgingClient", false, oppfolgingClient),
                new SelfTestCheck("PersonClient", false, veilarbpersonClient),
                new SelfTestCheck("RegistreringClient", false, registreringClient),
                new SelfTestCheck("SafClient", false, safClient),
                new SelfTestCheck("VeilederOgEnhetClient", false, veiledereOgEnhetClient),
                new SelfTestCheck("Ping database", true, () -> checkDbHealth(dataSourceHealthIndicator))
        );

        return new SelfTestChecks(selfTestChecks);
    }

    private HealthCheckResult checkDbHealth(DataSourceHealthIndicator dataSourceHealthIndicator) {
        Health health = dataSourceHealthIndicator.health();
        if (Status.UP.equals(health.getStatus())) {
            return HealthCheckResult.healthy();
        } else {
            return HealthCheckResult.unhealthy("Fikk ikke kontakt med databasen" + health.getDetails().toString());
        }
    }

    @Bean
    public SelfTestMeterBinder selfTestMeterBinder(SelfTestChecks selfTestChecks) {
        return new SelfTestMeterBinder(selfTestChecks);
    }

}

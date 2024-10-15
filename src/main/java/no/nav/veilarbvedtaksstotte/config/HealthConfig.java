package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.selftest.SelfTestCheck;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestMeterBinder;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.AiaBackendClient;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient;
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client;
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClient;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;

@Configuration
public class HealthConfig {

    @Bean
    public DataSourceHealthIndicator dataSourceHealthIndicator(DataSource dataSource) {
        return new DataSourceHealthIndicator(dataSource, "SELECT 1");
    }

    @Bean
    public SelfTestChecks selfTestChecks(VeilarbarenaClient arenaClient,
                                         AiaBackendClient aiaBackendClient,
                                         VeilarboppfolgingClient oppfolgingClient,
                                         VeilarbpersonClient veilarbpersonClient,
                                         VeilarbregistreringClient registreringClient,
                                         PdfClient pdfClient,
                                         SafClient safClient,
                                         Norg2Client norg2Client,
                                         VeilarbveilederClient veilarbveilederClient,
                                         DokarkivClient dokarkivClient,
                                         DokdistribusjonClient dokdistribusjonClient,
                                         RegoppslagClient regoppslagClient,
                                         DataSourceHealthIndicator dataSourceHealthIndicator) {


        ArrayList<SelfTestCheck> selfTestChecks = new ArrayList<>(Arrays.asList(
                new SelfTestCheck("ArenaClient", false, arenaClient),
                new SelfTestCheck("pto-pdfgen", false, pdfClient),
                new SelfTestCheck("Norg2", true, norg2Client),
                new SelfTestCheck("EgenvurderingClient", false, aiaBackendClient),
                new SelfTestCheck("OppfolgingClient", false, oppfolgingClient),
                new SelfTestCheck("PersonClient", false, veilarbpersonClient),
                new SelfTestCheck("RegistreringClient (via veilarbperson)", false, registreringClient),
                new SelfTestCheck("SafClient", false, safClient),
                new SelfTestCheck("veilarbveileder", false, veilarbveilederClient),
                new SelfTestCheck("Ping database", true, () -> checkDbHealth(dataSourceHealthIndicator)),
                new SelfTestCheck("DokarkivClient", false, dokarkivClient),
                new SelfTestCheck("DokdistribusjonClient", false, dokdistribusjonClient),
                new SelfTestCheck("RegoppslagClient", false, regoppslagClient)
        ));


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

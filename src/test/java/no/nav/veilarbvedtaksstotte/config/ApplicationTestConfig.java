package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.abac.AbacClient;
import no.nav.common.abac.Pep;
import no.nav.common.aktorregisterklient.AktorregisterKlient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.nais.NaisUtils;
import no.nav.veilarbvedtaksstotte.mock.AbacClientMock;
import no.nav.veilarbvedtaksstotte.mock.AktorregisterKlientMock;
import no.nav.veilarbvedtaksstotte.mock.MetricsClientMock;
import no.nav.veilarbvedtaksstotte.mock.PepMock;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;

@Configuration
@EnableConfigurationProperties(EnvironmentProperties.class)
public class ApplicationTestConfig {

    @Bean
    public NaisUtils.Credentials serviceUserCredentials() {
        return mock(NaisUtils.Credentials.class);
    }

    @Bean
    public AktorregisterKlient aktorregisterKlient() {
        return new AktorregisterKlientMock();
    }

    @Bean
    public AbacClient abacClient() {
        return new AbacClientMock();
    }

    @Bean
    public Pep veilarbPep() {
        return new PepMock();
    }

    @Bean
    public MetricsClient metricsClient() {
        return new MetricsClientMock();
    }

    @Bean
    public DataSource dataSource() {
        return SingletonPostgresContainer.init().getDataSource();
    }

}

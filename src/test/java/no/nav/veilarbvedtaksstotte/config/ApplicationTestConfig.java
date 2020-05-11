package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.abac.AbacClient;
import no.nav.common.abac.Pep;
import no.nav.common.aktorregisterklient.AktorregisterKlient;
import no.nav.common.metrics.MetricsClient;
import no.nav.veilarbvedtaksstotte.mock.AbacClientMock;
import no.nav.veilarbvedtaksstotte.mock.AktorregisterKlientMock;
import no.nav.veilarbvedtaksstotte.mock.MetricsClientMock;
import no.nav.veilarbvedtaksstotte.mock.PepMock;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(EnvironmentProperties.class)
public class ApplicationTestConfig {

    private final EnvironmentProperties environmentProperties;

    @Autowired
    public ApplicationTestConfig(EnvironmentProperties environmentProperties) {
        this.environmentProperties = environmentProperties;
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
        // TODO: Use datasource from config
//        HikariConfig config = new HikariConfig();
//        config.setJdbcUrl(properties.getDbUrl());
//        config.setMaximumPoolSize(1);
//        config.setMinimumIdle(1);
//        config.setUsername("postgres");
//        config.setPassword("qwerty");
        return SingletonPostgresContainer.init().getDataSource();
    }

}

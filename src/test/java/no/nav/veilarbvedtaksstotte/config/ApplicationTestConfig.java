package no.nav.veilarbvedtaksstotte.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.common.abac.AbacClient;
import no.nav.common.abac.Pep;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.utils.Credentials;
import no.nav.veilarbvedtaksstotte.kafka.KafkaTopics;
import no.nav.veilarbvedtaksstotte.mock.AbacClientMock;
import no.nav.veilarbvedtaksstotte.mock.MetricsClientMock;
import no.nav.veilarbvedtaksstotte.mock.PepMock;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;


@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
@Import({
        SwaggerConfig.class,
        ClientTestConfig.class,
        ControllerTestConfig.class,
        RepositoryTestConfig.class,
        ServiceTestConfig.class,
        KafkaTestConfig.class,
        FilterTestConfig.class,
        HealthConfig.class
})
public class ApplicationTestConfig {

    @Bean
    public KafkaTopics kafkaTopics() {
        return KafkaTopics.create("local");
    }

    @Bean
    public Credentials serviceUserCredentials() {
        return new Credentials("username", "password");
    }

    @Bean
    public AbacClient abacClient() {
        return new AbacClientMock();
    }

    @Bean
    public Pep veilarbPep(AbacClient abacClient) {
        return new PepMock(abacClient);
    }

    @Bean
    public MetricsClient metricsClient() {
        return new MetricsClientMock();
    }

    @Bean
    public DataSource dataSource() {
        return SingletonPostgresContainer.init().createDataSource();
    }

    @Bean
    public TransactionTemplate transactionTemplate(DataSource dataSource) {
        return new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public UnleashService unleashService() {
        return mock(UnleashService.class);
    }

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

}

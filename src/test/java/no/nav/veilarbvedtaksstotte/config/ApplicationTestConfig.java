package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.abac.AbacClient;
import no.nav.common.abac.Pep;
import no.nav.common.aktorregisterklient.AktorregisterKlient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.utils.Credentials;
import no.nav.veilarbvedtaksstotte.kafka.KafkaTopicProperties;
import no.nav.veilarbvedtaksstotte.mock.AbacClientMock;
import no.nav.veilarbvedtaksstotte.mock.AktorregisterKlientMock;
import no.nav.veilarbvedtaksstotte.mock.MetricsClientMock;
import no.nav.veilarbvedtaksstotte.mock.PepMock;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;


@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class,  KafkaTopicProperties.class})
@Import({
        CacheConfig.class,
        SwaggerConfig.class,
        ClientTestConfig.class,
        ControllerTestConfig.class,
        RepositoryTestConfig.class,
        ServiceTestConfig.class,
        KafkaTestConfig.class
})
public class ApplicationTestConfig {

    @Bean
    public KafkaTopicProperties kafkaTopicProperties() {
        KafkaTopicProperties props = new KafkaTopicProperties();
        props.setEndringPaAvsluttOppfolging("test1");
        props.setEndringPaOppfolgingBruker("test2");
        props.setVedtakSendt("test3");
        props.setVedtakStatusEndring("test4");

        return props;
    }

    @Bean
    public Credentials serviceUserCredentials() {
        return new Credentials("username", "password");
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

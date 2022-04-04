package no.nav.veilarbvedtaksstotte.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.common.abac.AbacClient;
import no.nav.common.abac.Pep;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.util.KafkaPropertiesBuilder;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.utils.Credentials;
import no.nav.veilarbvedtaksstotte.metrics.DokumentdistribusjonMeterBinder;
import no.nav.veilarbvedtaksstotte.mock.AbacClientMock;
import no.nav.veilarbvedtaksstotte.mock.MetricsClientMock;
import no.nav.veilarbvedtaksstotte.mock.PepMock;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import java.util.Map;
import java.util.Properties;

import static no.nav.veilarbvedtaksstotte.config.KafkaConfig.CONSUMER_GROUP_ID;
import static no.nav.veilarbvedtaksstotte.config.KafkaConfig.PRODUCER_CLIENT_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
@Import({
        ClientTestConfig.class,
        ControllerTestConfig.class,
        RepositoryTestConfig.class,
        ServiceTestConfig.class,
        FilterTestConfig.class,
        HealthConfig.class,
        KafkaConfig.class,
        DokumentdistribusjonMeterBinder.class
})
public class ApplicationTestConfig {

    public static final String KAFKA_IMAGE = "confluentinc/cp-kafka:5.4.3";

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
    public UnleashClient unleashClient() {
        UnleashClient unleashClient = mock(UnleashClient.class);
        when(unleashClient.checkHealth()).thenReturn(HealthCheckResult.healthy());
        return unleashClient;
    }

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }

    @Bean
    public LeaderElectionClient leaderElectionClient() {
        return () -> true;
    }

    @Bean
    public KafkaContainer kafkaContainer() {
        KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
        kafkaContainer.start();
        return kafkaContainer;
    }

    @Bean
    public KafkaConfig.EnvironmentContext kafkaConfigEnvContext(KafkaContainer kafkaContainer) {
        Properties consumerProperties = KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties(1000)
                .withConsumerGroupId(CONSUMER_GROUP_ID)
                .withBrokerUrl(kafkaContainer.getBootstrapServers())
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .build();

        Properties producerProperties = KafkaPropertiesBuilder.producerBuilder()
                .withBaseProperties()
                .withProducerId(PRODUCER_CLIENT_ID)
                .withBrokerUrl(kafkaContainer.getBootstrapServers())
                .withSerializers(ByteArraySerializer.class, ByteArraySerializer.class)
                .build();

        return new KafkaConfig.EnvironmentContext()
                .setOnPremConsumerClientProperties(consumerProperties)
                .setOnPremProducerClientProperties(producerProperties)
                .setAivenProducerClientProperties(producerProperties);
    }

    @Bean
    public KafkaConfig.KafkaAvroContext kafkaAvroContext() {
        return new KafkaConfig.KafkaAvroContext().setConfig(
                Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://testurl",
                        KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true));
    }

    @PostConstruct
    public void initJsonUtils() {
        JsonUtils.init();
    }
}

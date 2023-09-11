package no.nav.veilarbvedtaksstotte.config

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.getunleash.DefaultUnleash
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.common.abac.AbacClient
import no.nav.common.abac.Pep
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.health.HealthCheckResult
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.metrics.MetricsClient
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.common.utils.Credentials
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.veilarbvedtaksstotte.kafka.KafkaTestProducer
import no.nav.veilarbvedtaksstotte.metrics.DokumentdistribusjonMeterBinder
import no.nav.veilarbvedtaksstotte.mock.AbacClientMock
import no.nav.veilarbvedtaksstotte.mock.MetricsClientMock
import no.nav.veilarbvedtaksstotte.mock.PepMock
import no.nav.veilarbvedtaksstotte.mock.PoaoTilgangClientMock
import no.nav.veilarbvedtaksstotte.utils.JsonUtils.init
import no.nav.veilarbvedtaksstotte.utils.PostgresContainer
import no.nav.veilarbvedtaksstotte.utils.SingletonKafkaContainer
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.mockito.Mockito
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.KafkaContainer
import jakarta.annotation.PostConstruct
import javax.sql.DataSource

@Configuration
@EnableConfigurationProperties(EnvironmentProperties::class)
@Import(
    ClientTestConfig::class,
    ControllerTestConfig::class,
    RepositoryTestConfig::class,
    ServiceTestConfig::class,
    FilterTestConfig::class,
    HealthConfig::class,
    KafkaProducerConfig::class,
    KafkaConsumerConfig::class,
    DokumentdistribusjonMeterBinder::class
)
class ApplicationTestConfig {
    @Bean
    fun serviceUserCredentials(): Credentials {
        return Credentials("username", "password")
    }

    @Bean
    fun abacClient(): AbacClient {
        return AbacClientMock()
    }

    @Bean
    fun veilarbPep(abacClient: AbacClient): Pep {
        return PepMock(abacClient)
    }

    @Bean
    fun poaoTilgangClient(): PoaoTilgangClient {
        return PoaoTilgangClientMock()
    }

    @Bean
    fun metricsClient(): MetricsClient {
        return MetricsClientMock()
    }

    @Bean
    fun postgresContainer(): PostgresContainer {
        return SingletonPostgresContainer.init()
    }

    @Bean
    fun dataSource(postgresContainer: PostgresContainer): DataSource {
        return postgresContainer.createDataSource()
    }

    @Bean
    fun transactionTemplate(postgresContainer: PostgresContainer): TransactionTemplate {
        return TransactionTemplate(DataSourceTransactionManager(postgresContainer.createDataSource()))
    }

    @Bean
    fun jdbcTemplate(postgresContainer: PostgresContainer): JdbcTemplate {
        return JdbcTemplate(postgresContainer.createDataSource())
    }

    @Bean
    fun unleashClient(): DefaultUnleash {
        return Mockito.mock(DefaultUnleash::class.java)
    }

    @Bean
    fun meterRegistry(): MeterRegistry {
        return SimpleMeterRegistry()
    }

    @Bean
    fun authContextHolder(): AuthContextHolder {
        return AuthContextHolderThreadLocal.instance()
    }

    @Bean
    fun leaderElectionClient(): LeaderElectionClient {
        return LeaderElectionClient { true }
    }

    // destroyMethod satt til "" hindrer at KafkaContainer, som er AutoCloseable,
    // stoppes når ApplicationContext for en test lukkes:
    @Bean(destroyMethod = "")
    fun kafkaContainer(): KafkaContainer {
        return SingletonKafkaContainer.init()
    }

    @Bean
    fun kafkaConfigEnvContext(kafkaContainer: KafkaContainer): KafkaEnvironmentContext {
        val consumerProperties = KafkaPropertiesBuilder.consumerBuilder()
            .withBaseProperties(1000)
            .withConsumerGroupId(KafkaConsumerConfig.CONSUMER_GROUP_ID)
            .withBrokerUrl(kafkaContainer.bootstrapServers)
            .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
            .build()
        val producerProperties = KafkaPropertiesBuilder.producerBuilder()
            .withBaseProperties()
            .withProducerId(KafkaProducerConfig.PRODUCER_CLIENT_ID)
            .withBrokerUrl(kafkaContainer.bootstrapServers)
            .withSerializers(
                ByteArraySerializer::class.java, ByteArraySerializer::class.java
            )
            .build()
        return KafkaEnvironmentContext(
            onPremConsumerClientProperties = consumerProperties,
            aivenConsumerClientProperties = consumerProperties,
            onPremProducerClientProperties = producerProperties,
            aivenProducerClientProperties = producerProperties
        )
    }

    @Bean
    fun kafkaAvroContext(): KafkaAvroContext {
        return KafkaAvroContext(
            mapOf(
                Pair(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://testurl"),
                Pair(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)
            )
        )
    }

    @Bean
    fun azureAdOnBehalfOfTokenClient(): AzureAdOnBehalfOfTokenClient {
        return Mockito.mock(AzureAdOnBehalfOfTokenClient::class.java)
    }

    @Bean
    fun kafkaTestProducer(kafkaContainer: KafkaContainer): KafkaTestProducer {
        return KafkaTestProducer(
            mapOf(
                Pair(BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers),
                Pair(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java),
                Pair(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            )
        )
    }

    @PostConstruct
    fun initJsonUtils() {
        init()
    }

    companion object {
        const val KAFKA_IMAGE = "confluentinc/cp-kafka:5.4.3"
    }
}

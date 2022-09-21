package no.nav.veilarbvedtaksstotte.config

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.common.abac.AbacClient
import no.nav.common.abac.Pep
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.featuretoggle.UnleashClient
import no.nav.common.health.HealthCheckResult
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.metrics.MetricsClient
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.common.utils.Credentials
import no.nav.veilarbvedtaksstotte.config.KafkaConfig.EnvironmentContext
import no.nav.veilarbvedtaksstotte.config.KafkaConfig.KafkaAvroContext
import no.nav.veilarbvedtaksstotte.kafka.KafkaTestProducer
import no.nav.veilarbvedtaksstotte.metrics.DokumentdistribusjonMeterBinder
import no.nav.veilarbvedtaksstotte.mock.AbacClientMock
import no.nav.veilarbvedtaksstotte.mock.MetricsClientMock
import no.nav.veilarbvedtaksstotte.mock.PepMock
import no.nav.veilarbvedtaksstotte.utils.JsonUtils.init
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
import org.testcontainers.utility.DockerImageName
import javax.annotation.PostConstruct
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
    KafkaConfig::class,
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
    fun veilarbPep(abacClient: AbacClient?): Pep {
        return PepMock(abacClient)
    }

    @Bean
    fun metricsClient(): MetricsClient {
        return MetricsClientMock()
    }

    @Bean
    fun dataSource(): DataSource {
        return SingletonPostgresContainer.init().createDataSource()
    }

    @Bean
    fun transactionTemplate(dataSource: DataSource): TransactionTemplate {
        return TransactionTemplate(DataSourceTransactionManager(dataSource))
    }

    @Bean
    fun jdbcTemplate(dataSource: DataSource): JdbcTemplate {
        return JdbcTemplate(dataSource)
    }

    @Bean
    fun unleashClient(): UnleashClient {
        val unleashClient = Mockito.mock(UnleashClient::class.java)
        Mockito.`when`(unleashClient.checkHealth()).thenReturn(HealthCheckResult.healthy())
        return unleashClient
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

    @Bean
    fun kafkaContainer(): KafkaContainer {
        val kafkaContainer = KafkaContainer(DockerImageName.parse(KAFKA_IMAGE))
        kafkaContainer.start()
        return kafkaContainer
    }

    @Bean
    fun kafkaConfigEnvContext(kafkaContainer: KafkaContainer): EnvironmentContext {
        val consumerProperties = KafkaPropertiesBuilder.consumerBuilder()
            .withBaseProperties(1000)
            .withConsumerGroupId(KafkaConfig.CONSUMER_GROUP_ID)
            .withBrokerUrl(kafkaContainer.bootstrapServers)
            .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
            .build()
        val producerProperties = KafkaPropertiesBuilder.producerBuilder()
            .withBaseProperties()
            .withProducerId(KafkaConfig.PRODUCER_CLIENT_ID)
            .withBrokerUrl(kafkaContainer.bootstrapServers)
            .withSerializers(
                ByteArraySerializer::class.java, ByteArraySerializer::class.java
            )
            .build()
        return EnvironmentContext()
            .setOnPremConsumerClientProperties(consumerProperties)
            .setAivenConsumerClientProperties(consumerProperties)
            .setOnPremProducerClientProperties(producerProperties)
            .setAivenProducerClientProperties(producerProperties)
    }

    @Bean
    fun kafkaAvroContext(): KafkaAvroContext {
        return KafkaAvroContext().setConfig(
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
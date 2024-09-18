package no.nav.veilarbvedtaksstotte.config

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import jakarta.annotation.PostConstruct
import lombok.extern.slf4j.Slf4j
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.kafka.util.KafkaEnvironmentVariables
import no.nav.common.kafka.util.KafkaPropertiesPreset.aivenByteProducerProperties
import no.nav.common.kafka.util.KafkaPropertiesPreset.aivenDefaultConsumerProperties
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.common.utils.EnvironmentUtils.getRequiredProperty
import no.nav.veilarbvedtaksstotte.utils.JsonUtils.init
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Slf4j
@Configuration
@EnableScheduling
@EnableConfigurationProperties(EnvironmentProperties::class)
class ApplicationConfig {

    @Bean
    fun azureAdMachineToMachineTokenClient(): AzureAdMachineToMachineTokenClient {
        return AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .buildMachineToMachineTokenClient()
    }

    @Bean
    fun azureAdOnBehalfOfTokenClient(): AzureAdOnBehalfOfTokenClient {
        return AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .buildOnBehalfOfTokenClient()
    }

    @Bean
    fun authContextHolder(): AuthContextHolder {
        return AuthContextHolderThreadLocal.instance()
    }

    @Bean
    fun kafkaConfigEnvContext(
        kafkaProperties: KafkaProperties,
    ): KafkaEnvironmentContext {
        return KafkaEnvironmentContext(
            aivenConsumerClientProperties = aivenDefaultConsumerProperties(KafkaConsumerConfig.CONSUMER_GROUP_ID),
            aivenProducerClientProperties = aivenByteProducerProperties(KafkaProducerConfig.PRODUCER_CLIENT_ID)
        )
    }

    @Bean
    fun kafkaAvroContext(): KafkaAvroContext {

        val schemaRegistryConfig = HashMap<String, Any>()

        schemaRegistryConfig[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] =
            getRequiredProperty(KafkaEnvironmentVariables.KAFKA_SCHEMA_REGISTRY)

        schemaRegistryConfig[SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE] = "USER_INFO"

        schemaRegistryConfig[SchemaRegistryClientConfig.USER_INFO_CONFIG] = String.format(
            "%s:%s",
            getRequiredProperty(KafkaEnvironmentVariables.KAFKA_SCHEMA_REGISTRY_USER),
            getRequiredProperty(KafkaEnvironmentVariables.KAFKA_SCHEMA_REGISTRY_PASSWORD)
        )

        return KafkaAvroContext(schemaRegistryConfig)
    }

    @PostConstruct
    fun initJsonUtils() {
        init()
    }

    companion object {
        const val APPLICATION_NAME = "veilarbvedtaksstotte"
    }
}

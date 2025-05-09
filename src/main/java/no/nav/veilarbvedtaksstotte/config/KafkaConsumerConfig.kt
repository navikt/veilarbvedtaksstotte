package no.nav.veilarbvedtaksstotte.config

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG
import io.getunleash.DefaultUnleash
import io.micrometer.core.instrument.MeterRegistry
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder
import no.nav.common.kafka.consumer.util.ConsumerUtils
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import no.nav.common.kafka.spring.PostgresJdbcTemplateConsumerRepository
import no.nav.common.utils.EnvironmentUtils
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndringV2
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaSisteOppfolgingsperiode
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring
import no.nav.veilarbvedtaksstotte.service.KafkaConsumerService
import no.nav.veilarbvedtaksstotte.service.KafkaVedtakStatusEndringConsumer
import no.nav.veilarbvedtaksstotte.utils.KAFKA_KONSUMERING_GCP_SKRUDD_AV
import no.nav.veilarbvedtaksstotte.utils.LES_FRA_PDL_AKTOR_V2_TOPIC_SKRUDD_PAA
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import java.util.function.Consumer

@Configuration
@EnableConfigurationProperties(KafkaProperties::class)
class KafkaConsumerConfig {
    data class ConsumerAivenConfig(val configs: List<KafkaConsumerClientBuilder.TopicConfig<*, *>>)

    @Bean
    @Primary
    fun consumerAivenConfig(
        kafkaConsumerService: KafkaConsumerService,
        kafkaVedtakStatusEndringConsumer: KafkaVedtakStatusEndringConsumer,
        kafkaProperties: KafkaProperties,
        meterRegistry: MeterRegistry,
        kafkaConsumerRepository: KafkaConsumerRepository
    ): ConsumerAivenConfig {
        return ConsumerAivenConfig(
            getAivenConsumerTopicConfigs(
                kafkaConsumerService,
                kafkaVedtakStatusEndringConsumer,
                kafkaProperties,
                meterRegistry,
                kafkaConsumerRepository
            )
        )
    }

    @Bean
    fun consumerAivenConfigPdlAktorV2(
        kafkaConsumerService: KafkaConsumerService,
        kafkaProperties: KafkaProperties,
        meterRegistry: MeterRegistry,
        kafkaConsumerRepository: KafkaConsumerRepository
    ): ConsumerAivenConfig {
        val schemaRegistryUrl = EnvironmentUtils.getRequiredProperty("KAFKA_SCHEMA_REGISTRY");
        val keyDeserializer = Deserializers.aivenAvroDeserializer<String>()
        keyDeserializer.configure(
            mapOf(
                SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl,
                SPECIFIC_AVRO_READER_CONFIG to true
            ),
            true
        )
        val valueDeserializer = Deserializers.aivenAvroDeserializer<Aktor>()
        valueDeserializer.configure(
            mapOf(
                SPECIFIC_AVRO_READER_CONFIG to true,
                SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl,
            ),
            false
        )

        return ConsumerAivenConfig(
            listOf(
                KafkaConsumerClientBuilder.TopicConfig<String, Aktor>()
                    .withLogging()
                    .withMetrics(meterRegistry)
                    .withStoreOnFailure(kafkaConsumerRepository)
                    .withConsumerConfig(
                        kafkaProperties.pdlAktorV2Topic,
                        keyDeserializer,
                        valueDeserializer,
                        Consumer {
                            kafkaConsumerService.behandleKafkaMelding(
                                it,
                                kafkaConsumerService::behandlePdlAktorV2Melding
                            )
                        }
                    )
            )
        )
    }

    @Bean
    fun kafkaConsumerRepository(jdbcTemplate: JdbcTemplate): KafkaConsumerRepository {
        return PostgresJdbcTemplateConsumerRepository(jdbcTemplate)
    }

    @Bean(destroyMethod = "stop")
    @Primary
    fun aivenConsumerClient(
        environmentContext: KafkaEnvironmentContext,
        consumerAivenConfig: ConsumerAivenConfig,
        unleashService: DefaultUnleash
    ): KafkaConsumerClient {
        val aivenConsumerClient = KafkaConsumerClientBuilder.builder()
            .withProperties(environmentContext.aivenConsumerClientProperties)
            .withTopicConfigs(consumerAivenConfig.configs)
            .withToggle { unleashService.isEnabled(KAFKA_KONSUMERING_GCP_SKRUDD_AV) }
            .build()

        aivenConsumerClient.start()

        return aivenConsumerClient
    }

    @Bean(destroyMethod = "stop")
    fun aivenConsumerClientPdlAktorV2(
        environmentContext: KafkaEnvironmentContext,
        @Qualifier("consumerAivenConfigPdlAktorV2") consumerAivenConfig: ConsumerAivenConfig,
        unleashService: DefaultUnleash
    ): KafkaConsumerClient {
        val aivenConsumerClient = KafkaConsumerClientBuilder.builder()
            .withProperties(environmentContext.aivenConsumerClientProperties)
            .withTopicConfigs(consumerAivenConfig.configs)
            .withToggle {
                unleashService.isEnabled(KAFKA_KONSUMERING_GCP_SKRUDD_AV)
                        || !unleashService.isEnabled(LES_FRA_PDL_AKTOR_V2_TOPIC_SKRUDD_PAA)
            }
            .build()

        aivenConsumerClient.start()

        return aivenConsumerClient
    }

    @Bean(destroyMethod = "stop")
    fun consumerRecordProcessor(
        jdbcTemplate: JdbcTemplate,
        kafkaConsumerRepository: KafkaConsumerRepository,
        consumerAivenConfig: ConsumerAivenConfig
    ): KafkaConsumerRecordProcessor {

        val consumerRecordProcessor = getConsumerRecordProcessor(
            jdbcTemplate,
            kafkaConsumerRepository,
            consumerAivenConfig.configs
        )

        consumerRecordProcessor.start()

        return consumerRecordProcessor
    }

    companion object {
        const val CONSUMER_GROUP_ID = "veilarbvedtaksstotte-consumer"

        private fun getConsumerRecordProcessor(
            jdbcTemplate: JdbcTemplate,
            consumerRepository: KafkaConsumerRepository,
            topicConfigs: List<KafkaConsumerClientBuilder.TopicConfig<*, *>>
        ): KafkaConsumerRecordProcessor {
            return KafkaConsumerRecordProcessorBuilder
                .builder()
                .withLockProvider(JdbcTemplateLockProvider(jdbcTemplate))
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(ConsumerUtils.findConsumerConfigsWithStoreOnFailure(topicConfigs))
                .build()
        }

        private fun getAivenConsumerTopicConfigs(
            kafkaConsumerService: KafkaConsumerService,
            kafkaVedtakStatusEndringConsumer: KafkaVedtakStatusEndringConsumer,
            kafkaProperties: KafkaProperties,
            meterRegistry: MeterRegistry,
            consumerRepository: KafkaConsumerRepository
        ): List<KafkaConsumerClientBuilder.TopicConfig<*, *>> {
            val vedtakStatusEndringClientConfigBuilder =
                KafkaConsumerClientBuilder.TopicConfig<String, KafkaVedtakStatusEndring>()
                    .withLogging()
                    .withMetrics(meterRegistry)
                    .withStoreOnFailure(consumerRepository)
                    .withConsumerConfig(
                        kafkaProperties.vedtakStatusEndringTopic,
                        Deserializers.stringDeserializer(),
                        Deserializers.jsonDeserializer(KafkaVedtakStatusEndring::class.java),
                        Consumer {
                            kafkaConsumerService.behandleKafkaMelding(
                                it,
                                kafkaVedtakStatusEndringConsumer::konsumer
                            )
                        })

            val oppfolgingsbrukerEndringClientConfigBuilder =
                KafkaConsumerClientBuilder.TopicConfig<String, KafkaOppfolgingsbrukerEndringV2>()
                    .withLogging()
                    .withMetrics(meterRegistry)
                    .withStoreOnFailure(consumerRepository)
                    .withConsumerConfig(
                        kafkaProperties.endringPaOppfolgingsBrukerTopic,
                        Deserializers.stringDeserializer(),
                        Deserializers.jsonDeserializer(
                            KafkaOppfolgingsbrukerEndringV2::class.java
                        ),
                        Consumer {
                            kafkaConsumerService.behandleKafkaMelding(
                                it,
                                kafkaConsumerService::flyttingAvOppfolgingsbrukerTilNyEnhet
                            )
                        })

            val sisteOppfolgingsperiodeClientConfigBuilder =
                KafkaConsumerClientBuilder.TopicConfig<String, KafkaSisteOppfolgingsperiode>()
                    .withLogging()
                    .withMetrics(meterRegistry)
                    .withStoreOnFailure(consumerRepository)
                    .withConsumerConfig(
                        kafkaProperties.sisteOppfolgingsperiodeTopic,
                        Deserializers.stringDeserializer(),
                        Deserializers.jsonDeserializer(KafkaSisteOppfolgingsperiode::class.java),
                        Consumer {
                            kafkaConsumerService.behandleKafkaMelding(
                                it,
                                kafkaConsumerService::behandleSisteOppfolgingsperiode
                            )
                        }
                    )

            return listOf(
                vedtakStatusEndringClientConfigBuilder,
                oppfolgingsbrukerEndringClientConfigBuilder,
                sisteOppfolgingsperiodeClientConfigBuilder
            )
        }
    }
}


package no.nav.veilarbvedtaksstotte.config

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
import no.nav.common.utils.EnvironmentUtils.isDevelopment
import no.nav.veilarbvedtaksstotte.domain.kafka.ArenaVedtakRecord
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaAvsluttOppfolging
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndringV2
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring
import no.nav.veilarbvedtaksstotte.service.KafkaConsumerService
import no.nav.veilarbvedtaksstotte.service.KafkaVedtakStatusEndringConsumer
import no.nav.veilarbvedtaksstotte.service.UnleashService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties.Env
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import java.util.function.Consumer

@Configuration
@EnableConfigurationProperties(KafkaProperties::class)
class KafkaConsumerConfig {

    data class ConsumerOnPremConfig(val configs: List<KafkaConsumerClientBuilder.TopicConfig<*, *>>)
    data class ConsumerAivenConfig(val configs: List<KafkaConsumerClientBuilder.TopicConfig<*, *>>)

    @Bean
    fun consumerOnPremConfig(
        kafkaConsumerService: KafkaConsumerService,
        kafkaProperties: KafkaProperties,
        meterRegistry: MeterRegistry,
        kafkaConsumerRepository: KafkaConsumerRepository
    ): ConsumerOnPremConfig {
        return ConsumerOnPremConfig(
            getOnPremConsumerTopicConfigs(
                kafkaConsumerService,
                kafkaProperties,
                meterRegistry,
                kafkaConsumerRepository
            )
        )
    }

    @Bean
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
    fun kafkaConsumerRepository(jdbcTemplate: JdbcTemplate): KafkaConsumerRepository {
        return PostgresJdbcTemplateConsumerRepository(jdbcTemplate)
    }

    @Bean(destroyMethod = "stop")
    fun onPremConsumerClient(
        environmentContext: KafkaEnvironmentContext,
        consumerOnPremConfig: ConsumerOnPremConfig,
        unleashService: UnleashService
    ): KafkaConsumerClient {

        val onPremConsumerClient = KafkaConsumerClientBuilder.builder()
            .withProperties(environmentContext.onPremConsumerClientProperties)
            .withTopicConfigs(consumerOnPremConfig.configs)
            .withToggle { unleashService.isKafkaKonsumeringSkruddAv }
            .build()

        onPremConsumerClient.start()

        return onPremConsumerClient
    }

    @Bean(destroyMethod = "stop")
    fun aivenConsumerClient(
        environmentContext: KafkaEnvironmentContext,
        consumerAivenConfig: ConsumerAivenConfig,
        unleashService: UnleashService
    ): KafkaConsumerClient {

        val aivenConsumerClient = KafkaConsumerClientBuilder.builder()
            .withProperties(environmentContext.aivenConsumerClientProperties)
            .withTopicConfigs(consumerAivenConfig.configs)
            .withToggle { unleashService.isKafkaKonsumeringSkruddAv }
            .build()

        aivenConsumerClient.start()

        return aivenConsumerClient
    }

    @Bean(destroyMethod = "close")
    fun consumerRecordProcessor(
        jdbcTemplate: JdbcTemplate,
        kafkaConsumerRepository: KafkaConsumerRepository,
        consumerOnPremConfig: ConsumerOnPremConfig,
        consumerAivenConfig: ConsumerAivenConfig
    ): KafkaConsumerRecordProcessor {

        val consumerRecordProcessor = getConsumerRecordProcessor(
            jdbcTemplate,
            kafkaConsumerRepository,
            consumerOnPremConfig.configs + consumerAivenConfig.configs
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
                        Consumer { melding: ConsumerRecord<String, KafkaVedtakStatusEndring> ->
                            kafkaVedtakStatusEndringConsumer.konsumer(
                                melding
                            )
                        })

            val arenaVedtakClientConfigBuilder = KafkaConsumerClientBuilder.TopicConfig<String, ArenaVedtakRecord>()
                .withLogging()
                .withMetrics(meterRegistry)
                // Warning: Denne topicen bruker dato og tid som key, med presisjon på sekund. Det betyr at
                // meldinger for forskjellige brukere innenfor samme sekund kan blokkere for hverandre dersom
                // en melding feiler.
                .withStoreOnFailure(consumerRepository)
                .withConsumerConfig(
                    kafkaProperties.arenaVedtakTopic,
                    Deserializers.stringDeserializer(),
                    Deserializers.jsonDeserializer(ArenaVedtakRecord::class.java),
                    Consumer { arenaVedtakRecord: ConsumerRecord<String, ArenaVedtakRecord> ->
                        kafkaConsumerService.behandleArenaVedtak(
                            arenaVedtakRecord
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
                        Consumer { kafkaOppfolgingsbrukerEndringV2: ConsumerRecord<String, KafkaOppfolgingsbrukerEndringV2> ->
                            kafkaConsumerService.flyttingAvOppfolgingsbrukerTilNyEnhet(
                                kafkaOppfolgingsbrukerEndringV2
                            )
                        })

            val avsluttOppfolgingClientConfigBuilder =
                KafkaConsumerClientBuilder.TopicConfig<String, KafkaAvsluttOppfolging>()
                    .withLogging()
                    .withMetrics(meterRegistry)
                    .withStoreOnFailure(consumerRepository)
                    .withConsumerConfig(
                        kafkaProperties.endringPaAvsluttOppfolgingTopic,
                        Deserializers.stringDeserializer(),
                        Deserializers.jsonDeserializer(
                            KafkaAvsluttOppfolging::class.java
                        ),
                        Consumer { kafkaAvsluttOppfolging: ConsumerRecord<String, KafkaAvsluttOppfolging> ->
                            kafkaConsumerService.behandleEndringPaAvsluttOppfolging(
                                kafkaAvsluttOppfolging
                            )
                        })

            return if (isDevelopment().orElse(false)) {
                listOf(
                    vedtakStatusEndringClientConfigBuilder,
                    arenaVedtakClientConfigBuilder,
                    oppfolgingsbrukerEndringClientConfigBuilder,
                    avsluttOppfolgingClientConfigBuilder
                )
            }else{
                listOf(
                    vedtakStatusEndringClientConfigBuilder,
                    arenaVedtakClientConfigBuilder,
                    oppfolgingsbrukerEndringClientConfigBuilder
                )
            }
        }

        private fun getOnPremConsumerTopicConfigs(
            kafkaConsumerService: KafkaConsumerService,
            kafkaProperties: KafkaProperties,
            meterRegistry: MeterRegistry,
            consumerRepository: KafkaConsumerRepository
        ): List<KafkaConsumerClientBuilder.TopicConfig<*, *>> {

            val avsluttOppfolgingClientConfigBuilder =
                KafkaConsumerClientBuilder.TopicConfig<String, KafkaAvsluttOppfolging>()
                    .withLogging()
                    .withMetrics(meterRegistry)
                    .withStoreOnFailure(consumerRepository)
                    .withConsumerConfig(
                        kafkaProperties.endringPaAvsluttOppfolgingOnpremTopic,
                        Deserializers.stringDeserializer(),
                        Deserializers.jsonDeserializer(
                            KafkaAvsluttOppfolging::class.java
                        ),
                        Consumer { kafkaAvsluttOppfolging: ConsumerRecord<String, KafkaAvsluttOppfolging> ->
                            kafkaConsumerService.behandleEndringPaAvsluttOppfolging(
                                kafkaAvsluttOppfolging
                            )
                        })
            return if (isDevelopment().orElse(false)) {
                listOf()
            }else{
                listOf(avsluttOppfolgingClientConfigBuilder)
            }

        }
    }
}

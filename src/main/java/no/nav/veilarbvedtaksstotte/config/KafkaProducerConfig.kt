package no.nav.veilarbvedtaksstotte.config

import io.micrometer.core.instrument.MeterRegistry
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordProcessor
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRepository
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.common.kafka.spring.PostgresJdbcTemplateProducerRepository
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import java.util.*

@Configuration
@EnableConfigurationProperties(KafkaProperties::class)
class KafkaProducerConfig {

    @Bean
    fun producerRepository(jdbcTemplate: JdbcTemplate): KafkaProducerRepository {
        return PostgresJdbcTemplateProducerRepository(jdbcTemplate)
    }

    @Bean
    fun kafkaProducerRecordStorage(producerRepository: KafkaProducerRepository): KafkaProducerRecordStorage {
        return getProducerRecordStorage(producerRepository)
    }

    @Bean(destroyMethod = "close")
    fun aivenProducerRecordProcessor(
        environmentContext: KafkaEnvironmentContext,
        leaderElectionClient: LeaderElectionClient,
        producerRepository: KafkaProducerRepository,
        kafkaProperties: KafkaProperties,
        meterRegistry: MeterRegistry
    ): KafkaProducerRecordProcessor {

        val aivenProducerRecordProcessor = getProducerRecordProcessor(
            environmentContext.aivenProducerClientProperties,
            leaderElectionClient,
            producerRepository,
            meterRegistry,
            listOf(
                kafkaProperties.siste14aVedtakTopic,
                kafkaProperties.vedtakStatusEndringTopic,
                kafkaProperties.vedtakFattetDvhTopic,
                kafkaProperties.vedtakSendtTopic
            )
        )

        aivenProducerRecordProcessor.start()

        return aivenProducerRecordProcessor
    }

    companion object {

        const val PRODUCER_CLIENT_ID = "veilarbvedtaksstotte-producer"

        private fun getProducerRecordStorage(
            producerRepository: KafkaProducerRepository
        ): KafkaProducerRecordStorage {
            return KafkaProducerRecordStorage(producerRepository)
        }

        private fun getProducerRecordProcessor(
            properties: Properties,
            leaderElectionClient: LeaderElectionClient,
            producerRepository: KafkaProducerRepository,
            meterRegistry: MeterRegistry,
            topicWhitelist: List<String>
        ): KafkaProducerRecordProcessor {

            val producerClient = KafkaProducerClientBuilder.builder<ByteArray, ByteArray>()
                .withProperties(properties)
                .withMetrics(meterRegistry)
                .build()

            return KafkaProducerRecordProcessor(
                producerRepository,
                producerClient,
                leaderElectionClient,
                topicWhitelist
            )
        }
    }
}

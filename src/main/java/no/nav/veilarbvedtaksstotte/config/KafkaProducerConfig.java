package no.nav.veilarbvedtaksstotte.config;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordProcessor;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRepository;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.kafka.spring.PostgresJdbcTemplateProducerRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaProducerConfig {

    public final static String PRODUCER_CLIENT_ID = "veilarbvedtaksstotte-producer";

    @Bean
    public KafkaProducerRepository producerRepository(JdbcTemplate jdbcTemplate) {
        return new PostgresJdbcTemplateProducerRepository(jdbcTemplate);
    }

    @Bean
    public KafkaProducerRecordStorage kafkaProducerRecordStorage(KafkaProducerRepository producerRepository) {
        return getProducerRecordStorage(producerRepository);
    }

    @Bean(destroyMethod = "close")
    public KafkaProducerRecordProcessor onPremProducerRecordProcessor(
            KafkaEnvironmentContext environmentContext,
            LeaderElectionClient leaderElectionClient,
            KafkaProducerRepository producerRepository,
            KafkaProperties kafkaProperties,
            MeterRegistry meterRegistry
    ) {
        KafkaProducerRecordProcessor onPremProducerRecordProcessor = getProducerRecordProcessor(
                environmentContext.getOnPremProducerClientProperties(),
                leaderElectionClient,
                producerRepository,
                meterRegistry,
                List.of(
                        kafkaProperties.getVedtakSendtTopic()
                )
        );

        onPremProducerRecordProcessor.start();

        return onPremProducerRecordProcessor;
    }

    @Bean(destroyMethod = "close")
    public KafkaProducerRecordProcessor aivenProducerRecordProcessor(
            KafkaEnvironmentContext environmentContext,
            LeaderElectionClient leaderElectionClient,
            KafkaProducerRepository producerRepository,
            KafkaProperties kafkaProperties,
            MeterRegistry meterRegistry
    ) {
        KafkaProducerRecordProcessor aivenProducerRecordProcessor = getProducerRecordProcessor(
                environmentContext.getAivenProducerClientProperties(),
                leaderElectionClient,
                producerRepository,
                meterRegistry,
                List.of(
                        kafkaProperties.getSiste14aVedtakTopic(),
                        kafkaProperties.getVedtakStatusEndringTopic(),
                        kafkaProperties.getVedtakFattetDvhTopic()
                )
        );

        aivenProducerRecordProcessor.start();

        return aivenProducerRecordProcessor;
    }

    private static KafkaProducerRecordStorage getProducerRecordStorage(
            KafkaProducerRepository producerRepository
    ) {
        return new KafkaProducerRecordStorage(producerRepository);
    }

    private static KafkaProducerRecordProcessor getProducerRecordProcessor(
            Properties properties,
            LeaderElectionClient leaderElectionClient,
            KafkaProducerRepository producerRepository,
            MeterRegistry meterRegistry,
            List<String> topicWhitelist) {

        var producerClient = KafkaProducerClientBuilder.<byte[], byte[]>builder()
                .withProperties(properties)
                .withMetrics(meterRegistry)
                .build();

        return new KafkaProducerRecordProcessor(producerRepository, producerClient, leaderElectionClient, topicWhitelist);
    }
}

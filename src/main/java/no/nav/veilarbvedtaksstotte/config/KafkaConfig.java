package no.nav.veilarbvedtaksstotte.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.experimental.Accessors;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.PostgresConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder;
import no.nav.common.kafka.consumer.util.ConsumerUtils;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordProcessor;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRepository;
import no.nav.common.kafka.producer.feilhandtering.PostgresProducerRepository;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.veilarbvedtaksstotte.domain.kafka.ArenaVedtakRecord;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.service.KafkaConsumerService;
import no.nav.veilarbvedtaksstotte.utils.StoreOnFailureArenaTopicConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaConfig {

    @Data
    @Accessors(chain = true)
    public static class EnvironmentContext {
        Properties onPremConsumerClientProperties;
        Properties onPremProducerClientProperties;
        Properties aivenProducerClientProperties;
    }

    public final static String CONSUMER_GROUP_ID = "veilarbvedtaksstotte-consumer";
    public final static String PRODUCER_CLIENT_ID = "veilarbvedtaksstotte-producer";

    private final KafkaConsumerClient consumerClient;
    private final KafkaConsumerRecordProcessor consumerRecordProcessor;
    private final KafkaProducerRecordProcessor onPremProducerRecordProcessor;
    private final KafkaProducerRecordProcessor aivenProducerRecordProcessor;
    private final KafkaProducerRecordStorage producerRecordStorage;

    public KafkaConfig(
            EnvironmentContext environmentContext,
            LeaderElectionClient leaderElectionClient,
            JdbcTemplate jdbcTemplate,
            KafkaConsumerService kafkaConsumerService,
            KafkaProperties kafkaProperties,
            MeterRegistry meterRegistry
    ) {

        var consumerRepository = new PostgresConsumerRepository(jdbcTemplate.getDataSource());
        var producerRepository = new PostgresProducerRepository(jdbcTemplate.getDataSource());

        var topicConfigs = getTopicConfigs(kafkaConsumerService, kafkaProperties, meterRegistry, consumerRepository);

        consumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(environmentContext.getOnPremConsumerClientProperties())
                .withTopicConfigs(topicConfigs)
                .build();

        consumerRecordProcessor = getConsumerRecordProcessor(jdbcTemplate, consumerRepository, topicConfigs);

        producerRecordStorage = getProducerRecordStorage(producerRepository);

        onPremProducerRecordProcessor = getProducerRecordProcessor(
                environmentContext.getOnPremProducerClientProperties(),
                leaderElectionClient,
                producerRepository,
                meterRegistry,
                List.of(
                        kafkaProperties.getEndringPaAvsluttOppfolgingTopic(),
                        kafkaProperties.getEndringPaOppfolgingsBrukerTopic(),
                        kafkaProperties.getVedtakSendtTopic(),
                        kafkaProperties.getVedtakStatusEndringTopic(),
                        kafkaProperties.getArenaVedtakTopic()
                )
        );

        aivenProducerRecordProcessor = getProducerRecordProcessor(
                environmentContext.getAivenProducerClientProperties(),
                leaderElectionClient,
                producerRepository,
                meterRegistry,
                List.of(
                        kafkaProperties.getInnsatsbehovTopic()
                )
        );
    }

    private static KafkaProducerRecordProcessor getProducerRecordProcessor(
            Properties properties,
            LeaderElectionClient leaderElectionClient,
            PostgresProducerRepository producerRepository,
            MeterRegistry meterRegistry,
            List<String> topicWhitelist) {

        var producerClient = KafkaProducerClientBuilder.<byte[], byte[]>builder()
                .withProperties(properties)
                .withMetrics(meterRegistry)
                .build();

        return new KafkaProducerRecordProcessor(producerRepository, producerClient, leaderElectionClient, topicWhitelist);
    }

    private static KafkaConsumerRecordProcessor getConsumerRecordProcessor(
            JdbcTemplate jdbcTemplate,
            PostgresConsumerRepository consumerRepository,
            List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> topicConfigs
    ) {
        return KafkaConsumerRecordProcessorBuilder
                .builder()
                .withLockProvider(new JdbcTemplateLockProvider(jdbcTemplate))
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(getConsumerConfigsWithStoreOnFailure(topicConfigs))
                .build();
    }

    private static KafkaProducerRecordStorage getProducerRecordStorage(
            KafkaProducerRepository producerRepository
    ) {
        return new KafkaProducerRecordStorage(producerRepository);
    }

    private static List<TopicConsumerConfig<?, ?>> getConsumerConfigsWithStoreOnFailure(
            List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> topicConfigs
    ) {
        // Ville normalt filtrert consumers der .getConsumerRepository() != null (fra
        // ConsumerUtils.findConsumerConfigsWithStoreOnFailure), men her skal alle feilhåndeteres, og ArenaVedtakTopic
        // krever custom store on failure logikk i StoreOnFailureArenaTopicConsumer pga record key er en dato og ikke
        // unik per bruker. Derfor har ikke TopicConfig for denne et ConsumerRepository direkte.

        return topicConfigs.stream()
                .map(KafkaConsumerClientBuilder.TopicConfig::getConsumerConfig)
                .collect(Collectors.toList());
    }

    private static List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> getTopicConfigs(
            KafkaConsumerService kafkaConsumerService,
            KafkaProperties kafkaProperties,
            MeterRegistry meterRegistry,
            PostgresConsumerRepository consumerRepository) {
        return List.of(
                getArenaVedtakTopicConfig(kafkaProperties, kafkaConsumerService, meterRegistry, consumerRepository),
                new KafkaConsumerClientBuilder.TopicConfig<String, KafkaAvsluttOppfolging>()
                        .withLogging()
                        .withMetrics(meterRegistry)
                        .withStoreOnFailure(consumerRepository)
                        .withConsumerConfig(
                                kafkaProperties.getEndringPaAvsluttOppfolgingTopic(),
                                Deserializers.stringDeserializer(),
                                Deserializers.jsonDeserializer(KafkaAvsluttOppfolging.class),
                                kafkaConsumerService::behandleEndringPaAvsluttOppfolging
                        ),
                new KafkaConsumerClientBuilder.TopicConfig<String, KafkaOppfolgingsbrukerEndring>()
                        .withLogging()
                        .withMetrics(meterRegistry)
                        .withStoreOnFailure(consumerRepository)
                        .withConsumerConfig(
                                kafkaProperties.getEndringPaOppfolgingsBrukerTopic(),
                                Deserializers.stringDeserializer(),
                                Deserializers.jsonDeserializer(KafkaOppfolgingsbrukerEndring.class),
                                kafkaConsumerService::behandleEndringPaOppfolgingsbruker
                        )
        );
    }

    private static KafkaConsumerClientBuilder.TopicConfig<byte[], byte[]> getArenaVedtakTopicConfig(
            KafkaProperties kafkaProperties,
            KafkaConsumerService kafkaConsumerService,
            MeterRegistry meterRegistry,
            KafkaConsumerRepository consumerRepository
    ) {

        var arenaTopicConsumerConfig = new TopicConsumerConfig<>(
                kafkaProperties.getArenaVedtakTopic(),
                Deserializers.stringDeserializer(),
                Deserializers.jsonDeserializer(ArenaVedtakRecord.class),
                ConsumerUtils.toTopicConsumer(kafkaConsumerService::behandleArenaVedtak)
        );

        return new KafkaConsumerClientBuilder.TopicConfig<byte[], byte[]>()
                .withLogging()
                .withMetrics(meterRegistry)
                .withConsumerConfig(
                        arenaTopicConsumerConfig.getTopic(),
                        new ByteArrayDeserializer(),
                        new ByteArrayDeserializer(),
                        new StoreOnFailureArenaTopicConsumer(
                                ConsumerUtils.createTopicConsumer(arenaTopicConsumerConfig),
                                consumerRepository));
    }

    @Bean
    public KafkaProducerRecordStorage kafkaProducerRecordStorage() {
        return producerRecordStorage;
    }

    @PostConstruct
    public void start() {
        consumerClient.start();
        consumerRecordProcessor.start();
        onPremProducerRecordProcessor.start();
        aivenProducerRecordProcessor.start();
    }
}

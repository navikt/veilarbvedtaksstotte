package no.nav.veilarbvedtaksstotte.config;

import io.micrometer.core.instrument.MeterRegistry;
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
import no.nav.common.utils.Credentials;
import no.nav.veilarbvedtaksstotte.domain.kafka.ArenaVedtakRecord;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.service.KafkaConsumerService;
import no.nav.veilarbvedtaksstotte.utils.StoreOnFailureArenaTopicConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremByteProducerProperties;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultConsumerProperties;

@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaConfig {

    public final static String CONSUMER_GROUP_ID = "veilarbvedtaksstotte-consumer";
    public final static String PRODUCER_CLIENT_ID = "veilarbvedtaksstotte-producer";

    private final KafkaConsumerClient consumerClient;
    private final KafkaConsumerRecordProcessor consumerRecordProcessor;
    private final KafkaProducerRecordProcessor producerRecordProcessor;
    private final KafkaProducerRecordStorage<String, String> producerRecordStorage;

    public KafkaConfig(
            LeaderElectionClient leaderElectionClient,
            JdbcTemplate jdbcTemplate,
            KafkaConsumerService kafkaConsumerService,
            KafkaProperties kafkaProperties,
            MeterRegistry meterRegistry,
            Credentials credentials
    ) {

        var consumerRepository = new PostgresConsumerRepository(jdbcTemplate.getDataSource());
        var producerRepository = new PostgresProducerRepository(jdbcTemplate.getDataSource());

        var topicConfigs = getTopicConfigs(kafkaConsumerService, kafkaProperties, meterRegistry, consumerRepository);

        consumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(onPremDefaultConsumerProperties(CONSUMER_GROUP_ID, kafkaProperties.getBrokersUrl(), credentials))
                .withTopicConfigs(topicConfigs)
                .build();

        consumerRecordProcessor = getConsumerRecordProcessor(jdbcTemplate, consumerRepository, topicConfigs);

        producerRecordStorage = getProducerRecordStorage(producerRepository);

        producerRecordProcessor = getProducerRecordProcessor(
                onPremByteProducerProperties(PRODUCER_CLIENT_ID, kafkaProperties.getBrokersUrl(), credentials),
                leaderElectionClient,
                producerRepository,
                meterRegistry
        );
    }

    protected static KafkaProducerRecordProcessor getProducerRecordProcessor(
            Properties properties,
            LeaderElectionClient leaderElectionClient,
            PostgresProducerRepository producerRepository,
            MeterRegistry meterRegistry) {

        var producerClient = KafkaProducerClientBuilder.<byte[], byte[]>builder()
                .withProperties(properties)
                .withMetrics(meterRegistry)
                .build();

        return new KafkaProducerRecordProcessor(producerRepository, producerClient, leaderElectionClient);
    }

    protected static KafkaConsumerRecordProcessor getConsumerRecordProcessor(
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

    protected static KafkaProducerRecordStorage<String, String> getProducerRecordStorage(
            KafkaProducerRepository producerRepository
    ) {
        return new KafkaProducerRecordStorage<>(
                producerRepository,
                new StringSerializer(),
                new StringSerializer()
        );
    }

    protected static List<TopicConsumerConfig<?, ?>> getConsumerConfigsWithStoreOnFailure(
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

    protected static List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> getTopicConfigs(
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
    public KafkaProducerRecordStorage<String, String> kafkaProducerRecordStorage() {
        return producerRecordStorage;
    }

    @PostConstruct
    public void start() {
        consumerClient.start();
        consumerRecordProcessor.start();
        producerRecordProcessor.start();
    }
}

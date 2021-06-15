package no.nav.veilarbvedtaksstotte.config;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor;
import no.nav.common.kafka.consumer.feilhandtering.PostgresConsumerRepository;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordProcessor;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.common.kafka.producer.feilhandtering.PostgresProducerRepository;
import no.nav.common.kafka.util.KafkaPropertiesBuilder;
import no.nav.veilarbvedtaksstotte.service.KafkaConsumerService;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.PostConstruct;
import java.util.Properties;

import static no.nav.veilarbvedtaksstotte.config.KafkaConfig.*;

@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaTestConfig {

    public static final String KAFKA_IMAGE = "confluentinc/cp-kafka:5.4.3";
    private final KafkaConsumerClient consumerClient;
    private final KafkaConsumerRecordProcessor consumerRecordProcessor;
    private final KafkaProducerRecordProcessor producerRecordProcessor;
    private final KafkaProducerRecordStorage<String, String> producerRecordStorage;
    private final KafkaContainer kafkaContainer;


    public KafkaTestConfig(
            LeaderElectionClient leaderElectionClient,
            JdbcTemplate jdbcTemplate,
            KafkaConsumerService kafkaConsumerService,
            KafkaProperties kafkaProperties,
            MeterRegistry meterRegistry
    ) {

        kafkaContainer = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
        kafkaContainer.start();

        var consumerRepository = new PostgresConsumerRepository(jdbcTemplate.getDataSource());
        var producerRepository = new PostgresProducerRepository(jdbcTemplate.getDataSource());

        var topicConfigs = getTopicConfigs(kafkaConsumerService, kafkaProperties, meterRegistry, consumerRepository);

        consumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(KafkaPropertiesBuilder.consumerBuilder()
                        .withBaseProperties(1000)
                        .withConsumerGroupId(CONSUMER_GROUP_ID)
                        .withBrokerUrl(kafkaContainer.getBootstrapServers())
                        .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                        .build())
                .withTopicConfigs(topicConfigs)
                .build();

        consumerRecordProcessor = getConsumerRecordProcessor(jdbcTemplate, consumerRepository, topicConfigs);

        producerRecordStorage = getProducerRecordStorage(producerRepository);

        producerRecordProcessor = getProducerRecordProcessor(
                testByteProducerProperties(kafkaContainer),
                leaderElectionClient,
                producerRepository,
                meterRegistry
        );
    }

    private Properties testByteProducerProperties(KafkaContainer kafkaContainer) {
        return KafkaPropertiesBuilder.producerBuilder()
                .withBaseProperties()
                .withProducerId(PRODUCER_CLIENT_ID)
                .withBrokerUrl(kafkaContainer.getBootstrapServers())
                .withSerializers(ByteArraySerializer.class, ByteArraySerializer.class)
                .build();
    }

    @Bean
    public KafkaProducerRecordStorage<String, String> producerRecordStorage() {
        return producerRecordStorage;
    }

    @Bean
    public KafkaContainer kafkaContainer() {
        return kafkaContainer;
    }

    @PostConstruct
    public void start() {
        consumerClient.start();
        consumerRecordProcessor.start();
        producerRecordProcessor.start();
    }
}

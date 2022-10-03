package no.nav.veilarbvedtaksstotte.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.experimental.Accessors;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.common.kafka.spring.PostgresJdbcTemplateConsumerRepository;
import no.nav.veilarbvedtaksstotte.domain.kafka.ArenaVedtakRecord;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.service.KafkaConsumerService;
import no.nav.veilarbvedtaksstotte.service.KafkaVedtakStatusEndringConsumer;
import no.nav.veilarbvedtaksstotte.service.UnleashService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure;

@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaConsumerConfig {

    @Data
    @Accessors(chain = true)
    public static class ConsumerOnPremConfig {
        List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> configs;
    }

    @Data
    @Accessors(chain = true)
    public static class ConsumerAivenConfig {
        List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> configs;
    }

    public final static String CONSUMER_GROUP_ID = "veilarbvedtaksstotte-consumer";

    @Bean
    public ConsumerOnPremConfig consumerOnPremConfig(KafkaConsumerService kafkaConsumerService,
                                                     KafkaProperties kafkaProperties,
                                                     MeterRegistry meterRegistry,
                                                     KafkaConsumerRepository kafkaConsumerRepository) {
        return new ConsumerOnPremConfig().setConfigs(
                getOnPremConsumerTopicConfigs(
                        kafkaConsumerService,
                        kafkaProperties,
                        meterRegistry,
                        kafkaConsumerRepository
                )
        );
    }

    @Bean
    public ConsumerAivenConfig consumerAivenConfig(KafkaVedtakStatusEndringConsumer kafkaVedtakStatusEndringConsumer,
                                                   KafkaProperties kafkaProperties,
                                                   MeterRegistry meterRegistry,
                                                   KafkaConsumerRepository kafkaConsumerRepository) {
        return new ConsumerAivenConfig().setConfigs(
                getAivenConsumerTopicConfigs(
                        kafkaVedtakStatusEndringConsumer,
                        kafkaProperties,
                        meterRegistry,
                        kafkaConsumerRepository
                ));
    }

    @Bean
    public KafkaConsumerRepository kafkaConsumerRepository(JdbcTemplate jdbcTemplate) {
        return new PostgresJdbcTemplateConsumerRepository(jdbcTemplate);
    }

    @Bean(destroyMethod = "stop")
    public KafkaConsumerClient onPremConsumerClient(KafkaEnvironmentContext environmentContext,
                                                    ConsumerOnPremConfig consumerOnPremConfig,
                                                    UnleashService unleashService) {

        KafkaConsumerClient onPremConsumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(environmentContext.getOnPremConsumerClientProperties())
                .withTopicConfigs(consumerOnPremConfig.configs)
                .withToggle(unleashService::isKafkaKonsumeringSkruddAv)
                .build();


        onPremConsumerClient.start();

        return onPremConsumerClient;
    }

    @Bean(destroyMethod = "stop")
    public KafkaConsumerClient aivenConsumerClient(KafkaEnvironmentContext environmentContext,
                                                   ConsumerAivenConfig consumerAivenConfig,
                                                   UnleashService unleashService) {
        KafkaConsumerClient aivenConsumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(environmentContext.getAivenConsumerClientProperties())
                .withTopicConfigs(consumerAivenConfig.configs)
                .withToggle(unleashService::isKafkaKonsumeringSkruddAv)
                .build();

        aivenConsumerClient.start();

        return aivenConsumerClient;
    }

    @Bean(destroyMethod = "close")
    public KafkaConsumerRecordProcessor consumerRecordProcessor(JdbcTemplate jdbcTemplate,
                                                                KafkaConsumerRepository kafkaConsumerRepository,
                                                                ConsumerOnPremConfig consumerOnPremConfig,
                                                                ConsumerAivenConfig consumerAivenConfig) {
        KafkaConsumerRecordProcessor consumerRecordProcessor = getConsumerRecordProcessor(
                jdbcTemplate,
                kafkaConsumerRepository,
                Stream.concat(
                        consumerOnPremConfig.configs.stream(),
                        consumerAivenConfig.configs.stream()).collect(toList())
        );

        consumerRecordProcessor.start();

        return consumerRecordProcessor;
    }

    private static KafkaConsumerRecordProcessor getConsumerRecordProcessor(
            JdbcTemplate jdbcTemplate,
            KafkaConsumerRepository consumerRepository,
            List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> topicConfigs
    ) {
        return KafkaConsumerRecordProcessorBuilder
                .builder()
                .withLockProvider(new JdbcTemplateLockProvider(jdbcTemplate))
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(findConsumerConfigsWithStoreOnFailure(topicConfigs))
                .build();
    }

    private static List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> getAivenConsumerTopicConfigs(
            KafkaVedtakStatusEndringConsumer kafkaVedtakStatusEndringConsumer,
            KafkaProperties kafkaProperties,
            MeterRegistry meterRegistry,
            KafkaConsumerRepository consumerRepository
    ) {
        return List.of(
                new KafkaConsumerClientBuilder.TopicConfig<String, KafkaVedtakStatusEndring>()
                        .withLogging()
                        .withMetrics(meterRegistry)
                        .withStoreOnFailure(consumerRepository)
                        .withConsumerConfig(
                                kafkaProperties.getVedtakStatusEndringTopic(),
                                Deserializers.stringDeserializer(),
                                Deserializers.jsonDeserializer(KafkaVedtakStatusEndring.class),
                                kafkaVedtakStatusEndringConsumer::konsumer
                        )
        );
    }

    private static List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> getOnPremConsumerTopicConfigs(
            KafkaConsumerService kafkaConsumerService,
            KafkaProperties kafkaProperties,
            MeterRegistry meterRegistry,
            KafkaConsumerRepository consumerRepository) {
        return List.of(
                new KafkaConsumerClientBuilder.TopicConfig<String, ArenaVedtakRecord>()
                        .withLogging()
                        .withMetrics(meterRegistry)
                        // Warning: Denne topicen bruker dato og tid som key, med presisjon på sekund. Det betyr at
                        // meldinger for forskjellige brukere innenfor samme sekund kan blokkere for hverandre dersom
                        // en melding feiler.
                        .withStoreOnFailure(consumerRepository)
                        .withConsumerConfig(
                                kafkaProperties.getArenaVedtakTopic(),
                                Deserializers.stringDeserializer(),
                                Deserializers.jsonDeserializer(ArenaVedtakRecord.class),
                                kafkaConsumerService::behandleArenaVedtak
                        ),
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
}

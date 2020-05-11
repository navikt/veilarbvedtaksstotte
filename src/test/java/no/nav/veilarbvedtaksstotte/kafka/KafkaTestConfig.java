package no.nav.veilarbvedtaksstotte.kafka;

import no.nav.veilarbvedtaksstotte.domain.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.domain.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.veilarbvedtaksstotte.config.ApplicationConfig.KAFKA_BROKERS_URL_PROPERTY;
import static no.nav.veilarbvedtaksstotte.config.KafkaConsumerConfig.AVSLUTT_OPPFOLGING_CONTAINER_FACTORY_NAME;
import static no.nav.veilarbvedtaksstotte.config.KafkaConsumerConfig.OPPFOLGINGSBRUKER_ENDRING_CONTAINER_FACTORY_NAME;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.mockito.Mockito.mock;

@EnableKafka
@Configuration
@Import({AvsluttOppfolgingConsumer.class, OppfolgingsbrukerEndringConsumer.class})
public class KafkaTestConfig {

    public static final String TEST_AVSLUTT_OPPFOLGING_TOPIC_NAME = "avslutt-oppfolging";
    public static final String TEST_OPPFOLGINGSBRUKER_ENDRING_TOPIC_NAME = "oppfolgingsbruker-endring";
    public static final List<String> TOPICS = Arrays.asList(TEST_AVSLUTT_OPPFOLGING_TOPIC_NAME, TEST_OPPFOLGINGSBRUKER_ENDRING_TOPIC_NAME);

    @Bean
    public VedtakService vedtakService() {
        return mock(VedtakService.class);
    }

    @Bean
    public AvsluttOppfolgingConsumer.ConsumerParameters avsluttOppfolgingConsumerConsumerParameters() {
        return new AvsluttOppfolgingConsumer.ConsumerParameters(TEST_AVSLUTT_OPPFOLGING_TOPIC_NAME);
    }

    @Bean(name = AVSLUTT_OPPFOLGING_CONTAINER_FACTORY_NAME)
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, KafkaAvsluttOppfolging>> avsluttOppfolgingKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, KafkaAvsluttOppfolging> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public OppfolgingsbrukerEndringConsumer.ConsumerParameters oppfolgingsbrukerEndringConsumerParameters() {
        return new OppfolgingsbrukerEndringConsumer.ConsumerParameters(TEST_OPPFOLGINGSBRUKER_ENDRING_TOPIC_NAME);
    }

    @Bean(name = OPPFOLGINGSBRUKER_ENDRING_CONTAINER_FACTORY_NAME)
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, KafkaOppfolgingsbrukerEndring>> oppfolgingsbrukerEndringKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, KafkaOppfolgingsbrukerEndring> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    private static DefaultKafkaConsumerFactory consumerFactory() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, getRequiredProperty(KAFKA_BROKERS_URL_PROPERTY));
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(GROUP_ID_CONFIG, "veilarbvedtaksstotte-consumer");
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(MAX_POLL_INTERVAL_MS_CONFIG, 1000);
        return new DefaultKafkaConsumerFactory<>(props);
    }
}

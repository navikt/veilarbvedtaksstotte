package no.nav.fo.veilarbvedtaksstotte.kafka;

import no.nav.fo.veilarbvedtaksstotte.domain.KafkaAvsluttOppfolging;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaOppfolgingsbrukerEndring;
import no.nav.fo.veilarbvedtaksstotte.service.VedtakService;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.HashMap;

import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.KAFKA_BROKERS_URL_PROPERTY;
import static no.nav.fo.veilarbvedtaksstotte.config.KafkaConsumerConfig.AVSLUTT_OPPFOLGING_CONTAINER_FACTORY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.config.KafkaConsumerConfig.OPPFOLGINGSBRUKER_ENDRING_CONTAINER_FACTORY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG;
import static org.mockito.Mockito.mock;

@EnableKafka
@Configuration
@Import({AvsluttOppfolgingConsumer.class, OppfolgingsbrukerEndringConsumer.class})
public class KafkaTestConfig {

    public static final String KAFKA_TEST_TOPIC = "test-topic";

    @Bean
    public VedtakService vedtakService() {
        return mock(VedtakService.class);
    }

    @Bean
    public AvsluttOppfolgingConsumer.ConsumerParameters avsluttOppfolgingConsumerConsumerParameters() {
        return new AvsluttOppfolgingConsumer.ConsumerParameters(KAFKA_TEST_TOPIC);
    }

    @Bean(name = AVSLUTT_OPPFOLGING_CONTAINER_FACTORY_NAME)
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, KafkaAvsluttOppfolging>> avsluttOppfolgingKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, KafkaAvsluttOppfolging> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public OppfolgingsbrukerEndringConsumer.ConsumerParameters oppfolgingsbrukerEndringConsumerParameters() {
        return new OppfolgingsbrukerEndringConsumer.ConsumerParameters(KAFKA_TEST_TOPIC);
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

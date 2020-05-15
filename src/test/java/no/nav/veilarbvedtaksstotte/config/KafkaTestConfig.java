package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.domain.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.domain.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.kafka.KafkaConsumer;
import no.nav.veilarbvedtaksstotte.kafka.KafkaHelsesjekk;
import no.nav.veilarbvedtaksstotte.kafka.KafkaProducer;
import no.nav.veilarbvedtaksstotte.kafka.KafkaTopicProperties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.LoggingProducerListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.util.HashMap;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@EnableKafka
@Configuration
@Import({
        KafkaProducer.class,
        KafkaConsumer.class,
        KafkaHelsesjekk.class
})
public class KafkaTestConfig {

    private final KafkaTopicProperties kafkaTopicProperties;

    @Autowired
    public KafkaTestConfig(KafkaTopicProperties kafkaTopicProperties) {
        this.kafkaTopicProperties = kafkaTopicProperties;
    }

    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker() {
        EmbeddedKafkaBroker embeddedKafkaBroker = new EmbeddedKafkaBroker(1, true, kafkaTopicProperties.getAllTopics());
        return embeddedKafkaBroker;
    }

    @Bean(name = KafkaConfig.AVSLUTT_OPPFOLGING_CONTAINER_FACTORY_NAME)
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, KafkaAvsluttOppfolging>> avsluttOppfolgingKafkaListenerContainerFactory(EmbeddedKafkaBroker embeddedKafkaBroker) {
        System.out.println("BROKERS================================ " + embeddedKafkaBroker.getBrokersAsString());
        ConcurrentKafkaListenerContainerFactory<String, KafkaAvsluttOppfolging> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(embeddedKafkaBroker.getBrokersAsString()));
        return factory;
    }

    @Bean(name = KafkaConfig.OPPFOLGINGSBRUKER_ENDRING_CONTAINER_FACTORY_NAME)
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, KafkaOppfolgingsbrukerEndring>> oppfolgingsbrukerEndringKafkaListenerContainerFactory(EmbeddedKafkaBroker embeddedKafkaBroker) {
        System.out.println("BROKERS================================ " + embeddedKafkaBroker.getBrokersAsString());
        ConcurrentKafkaListenerContainerFactory<String, KafkaOppfolgingsbrukerEndring> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(embeddedKafkaBroker.getBrokersAsString()));
        return factory;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(EmbeddedKafkaBroker embeddedKafkaBroker) {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory(embeddedKafkaBroker.getBrokersAsString()));
        LoggingProducerListener<String, String> producerListener = new LoggingProducerListener<>();
        producerListener.setIncludeContents(false);
        template.setProducerListener(producerListener);
        return template;
    }

    private static DefaultKafkaConsumerFactory consumerFactory(String kafkaBrokersUrl) {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokersUrl);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(GROUP_ID_CONFIG, "veilarbvedtaksstotte-consumer");
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(MAX_POLL_INTERVAL_MS_CONFIG, 1000);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    private static ProducerFactory<String, String> producerFactory(String kafkaBrokersUrl) {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokersUrl);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "veilarbvedtaksstotte-producer");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return new DefaultKafkaProducerFactory<>(props);
    }
}

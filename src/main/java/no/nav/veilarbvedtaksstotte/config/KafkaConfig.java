package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.utils.Credentials;
import no.nav.veilarbvedtaksstotte.domain.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.domain.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.kafka.KafkaHelsesjekk;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.LoggingProducerListener;

import java.util.HashMap;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@EnableKafka
@Configuration
public class KafkaConfig {

    public static final String AVSLUTT_OPPFOLGING_CONTAINER_FACTORY_NAME = "avsluttOppfolgingKafkaListenerContainerFactory";

    public static final String OPPFOLGINGSBRUKER_ENDRING_CONTAINER_FACTORY_NAME = "oppfolgingsbrukerEndringKafkaListenerContainerFactory";

    private final KafkaHelsesjekk kafkaHelsesjekk;

    private final Credentials serviceUserCredentials;

    @Value("${spring.kafka.bootstrap-servers}")
    String brokersUrl;

    @Autowired
    public KafkaConfig(KafkaHelsesjekk kafkaHelsesjekk, Credentials serviceUserCredentials) {
        this.kafkaHelsesjekk = kafkaHelsesjekk;
        this.serviceUserCredentials = serviceUserCredentials;
    }

    @Bean(name = AVSLUTT_OPPFOLGING_CONTAINER_FACTORY_NAME)
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, KafkaAvsluttOppfolging>> avsluttOppfolgingKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, KafkaAvsluttOppfolging> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(kafkaConsumerProperties(brokersUrl, serviceUserCredentials)));
        factory.setErrorHandler(kafkaHelsesjekk);
        return factory;
    }

    @Bean(name = OPPFOLGINGSBRUKER_ENDRING_CONTAINER_FACTORY_NAME)
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, KafkaOppfolgingsbrukerEndring>> oppfolgingsbrukerEndringKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, KafkaOppfolgingsbrukerEndring> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(kafkaConsumerProperties(brokersUrl, serviceUserCredentials)));
        factory.setErrorHandler(kafkaHelsesjekk);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(kafkaProducerProperties(brokersUrl, serviceUserCredentials));
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);
        LoggingProducerListener<String, String> producerListener = new LoggingProducerListener<>();
        producerListener.setIncludeContents(false);
        template.setProducerListener(producerListener);
        return template;
    }

    private static HashMap<String, Object> kafkaConsumerProperties(String kafkaBrokersUrl, Credentials serviceUserCredentials) {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokersUrl);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + serviceUserCredentials.username + "\" password=\"" + serviceUserCredentials.password + "\";");
        props.put(GROUP_ID_CONFIG, "veilarbvedtaksstotte-consumer");
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(MAX_POLL_INTERVAL_MS_CONFIG, 5000);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    private static HashMap<String, Object> kafkaProducerProperties (String kafkaBrokersUrl, Credentials serviceUserCredentials) {
        HashMap<String, Object>  props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokersUrl);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + serviceUserCredentials.username + "\" password=\"" + serviceUserCredentials.password + "\";");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "veilarbvedtaksstotte-producer");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return props;
    }

}

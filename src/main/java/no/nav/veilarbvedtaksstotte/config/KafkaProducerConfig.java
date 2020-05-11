package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.nais.NaisUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.LoggingProducerListener;

import java.util.HashMap;

import static no.nav.veilarbvedtaksstotte.utils.KafkaUtils.requireKafkaTopicEnv;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@Profile("!local")
@Configuration
public class KafkaProducerConfig {

    public static final String KAFKA_TOPIC_VEDTAK_SENDT = "aapen-oppfolging-vedtakSendt-v1-" + requireKafkaTopicEnv();
    public static final String KAFKA_TOPIC_VEDTAK_STATUS_ENDRING = "aapen-oppfolging-vedtakStatusEndring-v1-" + requireKafkaTopicEnv();

    @Bean
    public static ProducerFactory<String, String> producerFactory(EnvironmentProperties properties, NaisUtils.Credentials serviceUserCredentials) {
        return new DefaultKafkaProducerFactory<>(producerProperties(properties.getKafkaBrokersUrl(), serviceUserCredentials)); }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);
        LoggingProducerListener<String, String> producerListener = new LoggingProducerListener<>();
        producerListener.setIncludeContents(false);
        template.setProducerListener(producerListener);
        return template;
    }

    static HashMap<String, Object> producerProperties (String kafkaBrokersUrl, NaisUtils.Credentials serviceUserCredentials) {
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

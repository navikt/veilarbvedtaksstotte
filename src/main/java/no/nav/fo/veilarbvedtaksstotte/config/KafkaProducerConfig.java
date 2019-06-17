package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.fo.veilarbvedtaksstotte.kafka.KafkaHelsesjekk;
import no.nav.fo.veilarbvedtaksstotte.kafka.VedtakSendtTemplate;
import no.nav.fo.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;

import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.KAFKA_BROKERS_URL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@Configuration
@Import({ KafkaHelsesjekk.class })
public class KafkaProducerConfig {

    public static final String KAFKA_TOPIC = "aapen-oppfolging-vedtakSendt-v1" + "-" + requireEnvironmentName();
    private static final String KAFKA_BROKERS = getRequiredProperty(KAFKA_BROKERS_URL_PROPERTY);
    private static final String USERNAME = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
    private static final String PASSWORD = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);

    @Bean
    public static ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProperties()); }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public VedtakSendtTemplate vedtakSendtTemplate(KafkaRepository kafkaRepository) {
        return new VedtakSendtTemplate(kafkaTemplate(), KAFKA_TOPIC, kafkaRepository);
    }

    static HashMap<String, Object> producerProperties () {
        HashMap<String, Object>  props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "veilarbvedtaksstotte-producer");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return props;
    }

}

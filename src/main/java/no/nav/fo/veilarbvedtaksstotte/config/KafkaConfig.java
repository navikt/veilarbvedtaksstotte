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
import java.util.Map;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;

@Configuration
@Import({ KafkaHelsesjekk.class })
public class KafkaConfig {

    public static final String KAFKA_TOPIC = "aapen-fo-vedtakSendt-v1";
    public static final String KAFKA_BROKERS_URL_PROPERTY = "KAFKA_BROKERS_URL";

    private static final String KAFKA_BROKERS = getRequiredProperty(KAFKA_BROKERS_URL_PROPERTY);
    private static final String USERNAME = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
    private static final String PASSWORD = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);

    @Bean
    public static Map<String, Object> producerConfigs() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "veilarbvedtaksstotte-producer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");

        return props;
    }

    @Bean
    public static ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public VedtakSendtTemplate vedtakSendtTemplate(KafkaRepository kafkaRepository) {
        String topic = KAFKA_TOPIC + "-" + requireEnvironmentName();
        return new VedtakSendtTemplate(kafkaTemplate(), topic, kafkaRepository);
    }

}

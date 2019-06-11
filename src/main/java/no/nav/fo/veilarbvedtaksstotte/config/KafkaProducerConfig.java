package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.fo.veilarbvedtaksstotte.kafka.KafkaHelsesjekk;
import no.nav.fo.veilarbvedtaksstotte.kafka.VedtakSendtTemplate;
import no.nav.fo.veilarbvedtaksstotte.repository.KafkaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;

@Configuration
@Import({ KafkaHelsesjekk.class })
public class KafkaProducerConfig {

    public static final String KAFKA_TOPIC = "aapen-oppfolging-vedtakSendt-v1" + "-" + requireEnvironmentName();

    @Bean
    public static ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(KafkaProperties.producerProperties()); }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public VedtakSendtTemplate vedtakSendtTemplate(KafkaRepository kafkaRepository) {
        return new VedtakSendtTemplate(kafkaTemplate(), KAFKA_TOPIC, kafkaRepository);
    }

}

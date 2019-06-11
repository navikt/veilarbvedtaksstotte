package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.fo.veilarbvedtaksstotte.domain.KafkaAvsluttOppfolging;
import no.nav.fo.veilarbvedtaksstotte.kafka.AvsluttOpfolgingTemplate;
import no.nav.fo.veilarbvedtaksstotte.kafka.KafkaHelsesjekk;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;

@Configuration
@Import({ KafkaHelsesjekk.class })
@EnableKafka
public class KafkaConsumerConfig {
    public static final String KAFKA_CONSUMER_TOPIC = "aapen-fo-endringPaaAvsluttOppfolging-v1-" + requireEnvironmentName();

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, KafkaAvsluttOppfolging>> kafkaListenerContainerFactory(KafkaHelsesjekk kafkaHelsesjekk) {
        ConcurrentKafkaListenerContainerFactory<String, KafkaAvsluttOppfolging> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setErrorHandler(kafkaHelsesjekk);
        return factory;
    }

    @Bean
    public AvsluttOpfolgingTemplate.ConsumerParameters consumerParameters() {
        return new AvsluttOpfolgingTemplate.ConsumerParameters(KAFKA_CONSUMER_TOPIC);
    }

    @Bean
    public ConsumerFactory<String, KafkaAvsluttOppfolging> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(KafkaProperties.consumerProperties());
    }
}

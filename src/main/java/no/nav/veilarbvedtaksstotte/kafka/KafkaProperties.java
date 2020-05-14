package no.nav.veilarbvedtaksstotte.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static no.nav.veilarbvedtaksstotte.utils.KafkaUtils.requireKafkaTopicEnv;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {

    private String brokersUrl;

    private String topicVedtakSendt = "aapen-oppfolging-vedtakSendt-v1-" + requireKafkaTopicEnv();

    private String topicVedtakStatusEndring = "aapen-oppfolging-vedtakStatusEndring-v1-" + requireKafkaTopicEnv();

    private String topicEndringPaAvsluttOppfolging = "aapen-fo-endringPaaAvsluttOppfolging-v1-" + requireKafkaTopicEnv();

    private String topicEndringPaOppfolgingBruker = "aapen-fo-endringPaaOppfoelgingsBruker-v1-" + requireKafkaTopicEnv();

}

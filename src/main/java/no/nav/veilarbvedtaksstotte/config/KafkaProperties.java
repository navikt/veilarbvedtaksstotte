package no.nav.veilarbvedtaksstotte.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {
    String brokersUrl;
    String endringPaAvsluttOppfolgingTopic;
    String endringPaOppfolgingsBrukerTopic;
    String vedtakSendtTopic;
    String vedtakStatusEndringTopic;
    String arenaVedtakTopic;
    String innsatsbehovTopic;
}

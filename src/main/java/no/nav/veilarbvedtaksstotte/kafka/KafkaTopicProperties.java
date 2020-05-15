package no.nav.veilarbvedtaksstotte.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka.topic")
public class KafkaTopicProperties {

    private String vedtakSendt;

    private String vedtakStatusEndring;

    private String endringPaAvsluttOppfolging;

    private String endringPaOppfolgingBruker;

    public String[] getAllTopics() {
        return new String[]{
                this.getEndringPaAvsluttOppfolging(),
                this.getEndringPaOppfolgingBruker(),
                this.getVedtakSendt(),
                this.getVedtakStatusEndring()
        };
    }

}

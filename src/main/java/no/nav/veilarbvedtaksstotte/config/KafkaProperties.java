package no.nav.veilarbvedtaksstotte.config;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {
    String brokersUrl;
    String endringPaAvsluttOppfolgingTopic;
    String endringPaOppfolgingsBrukerTopic;
    String vedtakSendtTopic;
    String vedtakStatusEndringTopic;
    String arenaVedtakTopic;
    String siste14aVedtakTopic;
    String vedtakFattetDvhTopic;

    public String getBrokersUrl() {
        return this.brokersUrl;
    }

    public String getEndringPaAvsluttOppfolgingTopic() {
        return this.endringPaAvsluttOppfolgingTopic;
    }

    public String getEndringPaOppfolgingsBrukerTopic() {
        return this.endringPaOppfolgingsBrukerTopic;
    }

    public String getVedtakSendtTopic() {
        return this.vedtakSendtTopic;
    }

    public String getVedtakStatusEndringTopic() {
        return this.vedtakStatusEndringTopic;
    }

    public String getArenaVedtakTopic() {
        return this.arenaVedtakTopic;
    }

    public String getSiste14aVedtakTopic() {
        return this.siste14aVedtakTopic;
    }

    public String getVedtakFattetDvhTopic() {
        return this.vedtakFattetDvhTopic;
    }
}

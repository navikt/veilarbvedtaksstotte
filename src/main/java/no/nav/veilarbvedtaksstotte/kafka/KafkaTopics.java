package no.nav.veilarbvedtaksstotte.kafka;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KafkaTopics {

    private String vedtakSendt;

    private String vedtakStatusEndring;

    private String endringPaAvsluttOppfolging;

    private String endringPaOppfolgingBruker;

    public static KafkaTopics create(String topicPrefix) {
        KafkaTopics kafkaTopics = new KafkaTopics();
        kafkaTopics.setEndringPaAvsluttOppfolging("aapen-fo-endringPaaAvsluttOppfolging-v1-" + topicPrefix);
        kafkaTopics.setEndringPaOppfolgingBruker("aapen-fo-endringPaaOppfoelgingsBruker-v1-" + topicPrefix);
        kafkaTopics.setVedtakSendt("aapen-oppfolging-vedtakSendt-v1-" + topicPrefix);
        kafkaTopics.setVedtakStatusEndring("aapen-oppfolging-vedtakStatusEndring-v1-" + topicPrefix);
        return kafkaTopics;
    }

    public String[] getAllTopics() {
        return new String[]{
                this.getEndringPaAvsluttOppfolging(),
                this.getEndringPaOppfolgingBruker(),
                this.getVedtakSendt(),
                this.getVedtakStatusEndring()
        };
    }

}

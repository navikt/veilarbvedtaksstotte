package no.nav.veilarbvedtaksstotte.kafka;

import lombok.Getter;
import lombok.Setter;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;

@Getter
@Setter
public class KafkaTopics {

    public enum Topic {
        VEDTAK_SENDT, // Produce
        VEDTAK_STATUS_ENDRING, // Produce
        ENDRING_PA_AVSLUTT_OPPFOLGING, // Consume
        ENDRING_PA_OPPFOLGING_BRUKER // Consume
    }

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

    public String topicToStr(Topic topic) {
        switch (topic) {
            case VEDTAK_SENDT:
                return vedtakSendt;
            case VEDTAK_STATUS_ENDRING:
                return vedtakStatusEndring;
            default:
                throw new IllegalArgumentException(format("Klarte ikke å mappe %s til string", getName(topic)));
        }
    }

    public Topic strToTopic(String topicStr) {
        if (topicStr.equals(vedtakSendt)) {
            return Topic.VEDTAK_SENDT;
        } else if (topicStr.equals(vedtakStatusEndring)) {
            return Topic.VEDTAK_STATUS_ENDRING;
        }

        throw new IllegalArgumentException(format("Klarte ikke å mappe %s til enum", topicStr));
    }

}

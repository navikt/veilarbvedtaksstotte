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
        ENDRING_PA_OPPFOLGING_BRUKER, // Consume
        ARENA_VEDTAK // Consume
    }

    private String vedtakSendt;

    private String vedtakStatusEndring;

    private String endringPaAvsluttOppfolging;

    private String endringPaOppfolgingBruker;

    private String arenaVedtak;

    private KafkaTopics() {}

    public static KafkaTopics create(String topicPrefix) {
        KafkaTopics kafkaTopics = new KafkaTopics();
        kafkaTopics.setEndringPaAvsluttOppfolging("aapen-fo-endringPaaAvsluttOppfolging-v1-" + topicPrefix);
        kafkaTopics.setEndringPaOppfolgingBruker("aapen-fo-endringPaaOppfoelgingsBruker-v1-" + topicPrefix);
        kafkaTopics.setVedtakSendt("aapen-oppfolging-vedtakSendt-v1-" + topicPrefix);
        kafkaTopics.setVedtakStatusEndring("aapen-oppfolging-vedtakStatusEndring-v1-" + topicPrefix);
        kafkaTopics.setArenaVedtak("TODO-arena-vedtak-topic-name"); // TODO riktig navn på topic
        return kafkaTopics;
    }

    public String[] getAllTopics() {
        return new String[]{
                this.getEndringPaAvsluttOppfolging(),
                this.getEndringPaOppfolgingBruker(),
                this.getVedtakSendt(),
                this.getVedtakStatusEndring(),
                this.getArenaVedtak()
        };
    }

    public String topicToStr(Topic topic) {
        switch (topic) {
            case VEDTAK_SENDT:
                return vedtakSendt;
            case VEDTAK_STATUS_ENDRING:
                return vedtakStatusEndring;
            case ENDRING_PA_AVSLUTT_OPPFOLGING:
                return endringPaAvsluttOppfolging;
            case ENDRING_PA_OPPFOLGING_BRUKER:
                return endringPaOppfolgingBruker;
            default:
                throw new IllegalArgumentException(format("Klarte ikke å mappe %s til string", getName(topic)));
        }
    }

    public Topic strToTopic(String topicStr) {
        if (vedtakSendt.equals(topicStr)) {
            return Topic.VEDTAK_SENDT;
        } else if (vedtakStatusEndring.equals(topicStr)) {
            return Topic.VEDTAK_STATUS_ENDRING;
        } else if (endringPaAvsluttOppfolging.equals(topicStr)) {
            return Topic.ENDRING_PA_AVSLUTT_OPPFOLGING;
        } else if (endringPaOppfolgingBruker.equals(topicStr)) {
            return Topic.ENDRING_PA_OPPFOLGING_BRUKER;
        }

        throw new IllegalArgumentException(format("Klarte ikke å mappe %s til enum", topicStr));
    }

}

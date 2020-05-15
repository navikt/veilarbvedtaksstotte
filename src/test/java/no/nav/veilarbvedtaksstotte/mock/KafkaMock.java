package no.nav.veilarbvedtaksstotte.mock;

import no.nav.veilarbvedtaksstotte.kafka.KafkaTopicProperties;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

public class KafkaMock {

    private final EmbeddedKafkaBroker embeddedKafkaBroker;

    public KafkaMock(KafkaTopicProperties kafkaTopicProperties) {
        System.out.println("KAFKA_MOCK========================================================");
        this.embeddedKafkaBroker = new EmbeddedKafkaBroker(1, true, getAllTopics(kafkaTopicProperties));
        this.embeddedKafkaBroker.afterPropertiesSet();
    }

    private static String[] getAllTopics(KafkaTopicProperties properties) {
        return new String[]{
                properties.getEndringPaAvsluttOppfolging(),
                properties.getEndringPaOppfolgingBruker(),
                properties.getVedtakSendt(),
                properties.getVedtakStatusEndring()
        };
    }

}

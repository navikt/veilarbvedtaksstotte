package no.nav.fo.veilarbvedtaksstotte.kafka;

import lombok.SneakyThrows;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaOppfolgingsbrukerEndring;
import no.nav.fo.veilarbvedtaksstotte.service.VedtakService;
import org.junit.Test;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static no.nav.fo.veilarbvedtaksstotte.kafka.KafkaTestConfig.KAFKA_TEST_TOPIC;
import static no.nav.json.JsonUtils.toJson;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class OppfolgingsbrukerEndringConsumerTest extends KafkaTest {

    @Inject
    VedtakService vedtakService;

    @Test
    public void meldinger_for_endret_oppfolgingsbruker_blir_konsumert() throws Exception {

        KafkaOppfolgingsbrukerEndring melding = new KafkaOppfolgingsbrukerEndring("1", "2");

        send(KAFKA_TEST_TOPIC, toJson(melding));
        verifiserAsynkront(5, TimeUnit.SECONDS, () ->
                verify(vedtakService).behandleOppfolgingsbrukerEndring(eq(melding))
        );
    }

    @SneakyThrows
    private void send(String topic, String message) {
        kafkaTemplate.send(topic, message).get(10, TimeUnit.SECONDS);
    }
}

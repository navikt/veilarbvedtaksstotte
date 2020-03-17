package no.nav.veilarbvedtaksstotte.kafka;

import lombok.SneakyThrows;
import no.nav.veilarbvedtaksstotte.domain.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.junit.Test;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static no.nav.json.JsonUtils.toJson;
import static no.nav.veilarbvedtaksstotte.kafka.KafkaTestConfig.TEST_OPPFOLGINGSBRUKER_ENDRING_TOPIC_NAME;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class OppfolgingsbrukerEndringConsumerTest extends KafkaTest {

    @Inject
    VedtakService vedtakService;

    @Test
    public void meldinger_for_endret_oppfolgingsbruker_blir_konsumert() {

        KafkaOppfolgingsbrukerEndring melding = new KafkaOppfolgingsbrukerEndring("1", "2");

        send(TEST_OPPFOLGINGSBRUKER_ENDRING_TOPIC_NAME, toJson(melding));
        verifiserAsynkront(10, TimeUnit.SECONDS, () ->
                verify(vedtakService).behandleOppfolgingsbrukerEndring(eq(melding))
        );
    }

    @SneakyThrows
    private void send(String topic, String message) {
        kafkaTemplate.send(topic, message).get(10, TimeUnit.SECONDS);
    }
}

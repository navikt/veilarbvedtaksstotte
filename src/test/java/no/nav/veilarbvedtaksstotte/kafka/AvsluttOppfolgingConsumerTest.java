package no.nav.veilarbvedtaksstotte.kafka;

import lombok.SneakyThrows;
import no.nav.veilarbvedtaksstotte.domain.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.veilarbvedtaksstotte.kafka.KafkaTestConfig.TEST_AVSLUTT_OPPFOLGING_TOPIC_NAME;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class AvsluttOppfolgingConsumerTest extends KafkaTest {

    @Autowired
    VedtakService vedtakService;

    @Test
    @Ignore // TODO: Fjern når kafka oppsett fungerer
    public void meldinger_for_avslutt_oppfolging_blir_konsumert() {

        KafkaAvsluttOppfolging melding = new KafkaAvsluttOppfolging("1", new Date());

        send(TEST_AVSLUTT_OPPFOLGING_TOPIC_NAME, toJson(melding));
        verifiserAsynkront(10, TimeUnit.SECONDS, () ->
                verify(vedtakService).behandleAvsluttOppfolging(eq(melding))
        );
    }

    @SneakyThrows
    private void send(String topic, String message) {
        kafkaTemplate.send(topic, message).get(10, TimeUnit.SECONDS);
    }
}

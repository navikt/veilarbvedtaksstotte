package no.nav.veilarbvedtaksstotte.kafka;

import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.veilarbvedtaksstotte.utils.TestUtils.verifiserAsynkront;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationTestConfig.class)
@ActiveProfiles("local")
public class KafkaConsumerTest {

    @MockBean
    VedtakService vedtakService;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    KafkaTopics kafkaTopics;

    @Test
    public void meldinger_for_avslutt_oppfolging_blir_konsumert() {
        KafkaAvsluttOppfolging melding = new KafkaAvsluttOppfolging("1", ZonedDateTime.now());

        kafkaTemplate.send(kafkaTopics.getEndringPaAvsluttOppfolging(), toJson(melding));

        verifiserAsynkront(10, TimeUnit.SECONDS, () ->
                verify(vedtakService).behandleAvsluttOppfolging(eq(melding))
        );
    }

    @Test
    public void meldinger_for_endret_oppfolgingsbruker_blir_konsumert() {
        KafkaOppfolgingsbrukerEndring melding = new KafkaOppfolgingsbrukerEndring("1", "2");

        kafkaTemplate.send(kafkaTopics.getEndringPaOppfolgingBruker(), toJson(melding));

        verifiserAsynkront(10, TimeUnit.SECONDS, () ->
                verify(vedtakService).behandleOppfolgingsbrukerEndring(eq(melding))
        );
    }

}

package no.nav.veilarbvedtaksstotte.kafka;

import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.veilarbvedtaksstotte.utils.TestUtils.verifiserAsynkront;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationTestConfig.class)
@ActiveProfiles("local")
public class AvsluttOppfolgingConsumerTest {

    @SpyBean
    VedtakService vedtakService;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    KafkaTopics kafkaTopics;

    @Test
    public void meldinger_for_avslutt_oppfolging_blir_konsumert() {
        KafkaAvsluttOppfolging melding = new KafkaAvsluttOppfolging("1", new Date());

        kafkaTemplate.send(kafkaTopics.getEndringPaAvsluttOppfolging(), toJson(melding));

        verifiserAsynkront(10, TimeUnit.SECONDS, () ->
                verify(vedtakService).behandleAvsluttOppfolging(eq(melding))
        );
    }

}

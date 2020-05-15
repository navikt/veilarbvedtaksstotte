package no.nav.veilarbvedtaksstotte.kafka;

import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationTestConfig.class)
@ActiveProfiles("local")
public class AvsluttOppfolgingConsumerTest {

    @Autowired
    VedtakService vedtakService;

    @Autowired
    KafkaProducer kafkaProducer;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Test
    public void meldinger_for_avslutt_oppfolging_blir_konsumert() {

//        KafkaAvsluttOppfolging melding = new KafkaAvsluttOppfolging("1", new Date());
//
//        kafkaTemplate.send("aapen-fo-endringPaaAvsluttOppfolging", toJson(melding));

//        verifiserAsynkront(10, TimeUnit.SECONDS, () ->
//                verify(vedtakService).behandleAvsluttOppfolging(eq(melding))
////        );
    }

//    @SneakyThrows
//    private void send(String topic, String message) {
//        kafkaTemplate.send(topic, message).get(10, TimeUnit.SECONDS);
//    }
}

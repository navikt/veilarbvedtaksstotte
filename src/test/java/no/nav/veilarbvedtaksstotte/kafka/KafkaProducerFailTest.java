package no.nav.veilarbvedtaksstotte.kafka;

import kafka.server.KafkaServer;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

import static no.nav.veilarbvedtaksstotte.utils.TestUtils.verifiserAsynkront;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationTestConfig.class)
@ActiveProfiles("local")
public class KafkaProducerFailTest {

    @MockBean
    KafkaRepository kafkaRepository;

    @Autowired
    KafkaProducer kafkaProducer;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    public void skal_lagre_melding_hvis_publisering_feiler() {
        String aktorId = "11111111";

        KafkaVedtakSendt kafkaVedtakSendt = new KafkaVedtakSendt()
                .setAktorId(aktorId);

        String vedtakSendtJson = JsonUtils.toJson(kafkaVedtakSendt);

        embeddedKafkaBroker.getKafkaServers().forEach(KafkaServer::shutdown);

        kafkaProducer.sendVedtakSendt(kafkaVedtakSendt);

        verifiserAsynkront(10, TimeUnit.SECONDS, () ->
                verify(kafkaRepository, times(1))
                        .lagreFeiletProdusertKafkaMelding(eq(KafkaTopics.Topic.VEDTAK_SENDT), eq(aktorId), eq(vedtakSendtJson))
        );
    }

}

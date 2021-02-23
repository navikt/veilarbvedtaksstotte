package no.nav.veilarbvedtaksstotte.kafka;

import no.nav.common.json.JsonUtils;
import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.veilarbvedtaksstotte.service.InnsatsbehovService;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.veilarbvedtaksstotte.kafka.KafkaConfig.CONSUMER_GROUP_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestUtils.verifiserAsynkront;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ActiveProfiles("local")
@SpringBootTest(classes = ApplicationTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class KafkaConsumerFailTest {

    @MockBean
    KafkaRepository kafkaRepository;

    @MockBean
    VedtakService vedtakService;

    @MockBean
    InnsatsbehovService innsatsbehovService;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @SpyBean
    KafkaTopics kafkaTopics;

    @Test
    public void skal_lagre_melding_og_acke_etter_feilet_konsumering() {
        String aktorId = "111111111";
        KafkaAvsluttOppfolging melding = new KafkaAvsluttOppfolging(aktorId, ZonedDateTime.now());

        String jsonPayload = JsonUtils.toJson(melding);

        doThrow(new RuntimeException()).when(innsatsbehovService).behandleAvsluttOppfolging(any());

        kafkaTemplate.send(kafkaTopics.getEndringPaAvsluttOppfolging(), aktorId, toJson(melding));

        verifiserAsynkront(10, TimeUnit.SECONDS, () -> {
            embeddedKafkaBroker.doWithAdmin((admin) -> {
                try {
                    Map<TopicPartition, OffsetAndMetadata> topicMap = admin.listConsumerGroupOffsets(CONSUMER_GROUP_ID)
                            .partitionsToOffsetAndMetadata()
                            .get();

                    long offset = topicMap.get(new TopicPartition(kafkaTopics.getEndringPaAvsluttOppfolging(), 1)).offset();

                    assertEquals(1, offset);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });

            verify(kafkaRepository, times(1))
                    .lagreFeiletKonsumertKafkaMelding(
                            eq(KafkaTopics.Topic.ENDRING_PA_AVSLUTT_OPPFOLGING),
                            eq(aktorId),
                            eq(jsonPayload),
                            eq(0L)
                    );
        });
    }

    @Test
    public void skal_nacke_hvis_konsumering_og_lagring_feiler() throws InterruptedException {
        String aktorId = "111111111";
        KafkaAvsluttOppfolging melding = new KafkaAvsluttOppfolging(aktorId, ZonedDateTime.now());

        doThrow(new RuntimeException()).when(innsatsbehovService).behandleAvsluttOppfolging(any());

        doThrow(new RuntimeException()).when(kafkaTopics).strToTopic(any());

        kafkaTemplate.send(kafkaTopics.getEndringPaAvsluttOppfolging(), aktorId, toJson(melding));

        Thread.sleep(3000);

        embeddedKafkaBroker.doWithAdmin((admin) -> {
            try {
                Map<TopicPartition, OffsetAndMetadata> topicMap = admin.listConsumerGroupOffsets(CONSUMER_GROUP_ID)
                        .partitionsToOffsetAndMetadata()
                        .get();

                assertNull(topicMap.get(new TopicPartition(kafkaTopics.getEndringPaAvsluttOppfolging(), 1)));
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });

        verify(kafkaRepository, never()).lagreFeiletKonsumertKafkaMelding(any(), any(), any(), anyLong());
    }

}

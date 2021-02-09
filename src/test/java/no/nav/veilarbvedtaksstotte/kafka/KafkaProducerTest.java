package no.nav.veilarbvedtaksstotte.kafka;

import no.nav.common.json.JsonUtils;
import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.kafka.dto.VedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.veilarbvedtaksstotte.repository.domain.FeiletKafkaMelding;
import no.nav.veilarbvedtaksstotte.repository.domain.MeldingType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationTestConfig.class)
@ActiveProfiles("local")
public class KafkaProducerTest {

    @MockBean
    KafkaRepository kafkaRepository;

    @Autowired
    KafkaProducer kafkaProducer;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    KafkaTopics kafkaTopics;

    private KafkaMessageListenerContainer<String, String> container;

    private BlockingQueue<ConsumerRecord<String, String>> consumerRecords;

    @Before
    public void setUp() {
        consumerRecords = new LinkedBlockingQueue<>();

        ContainerProperties containerProperties = new ContainerProperties(kafkaTopics.getVedtakSendt(), kafkaTopics.getVedtakStatusEndring());

        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(
                "sender", "false", embeddedKafkaBroker);

        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        DefaultKafkaConsumerFactory<String, String> consumer = new DefaultKafkaConsumerFactory<>(consumerProperties);

        container = new KafkaMessageListenerContainer<>(consumer, containerProperties);
        container.setupMessageListener((MessageListener<String, String>) record -> consumerRecords.add(record));
        container.start();

        int totalPartitions = container.getAssignedPartitions().stream()
                .map(TopicPartition::partition)
                .reduce(0, Integer::sum);

        ContainerTestUtils.waitForAssignment(container, totalPartitions);
    }

    @After
    public void tearDown() {
        container.stop();
    }

    @Test
    public void skal_produsere_melding_pa_vedtak_sendt_topic() throws InterruptedException {
        String aktorId = "11111111";
        long id = 42;
        LocalDateTime vedtakSendt = LocalDateTime.now();
        String enhetId = "1234";
        Hovedmal hovedmal = Hovedmal.SKAFFE_ARBEID;
        Innsatsgruppe innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS;

        KafkaVedtakSendt kafkaVedtakSendt = new KafkaVedtakSendt()
                .setAktorId(aktorId)
                .setId(id)
                .setVedtakSendt(vedtakSendt)
                .setEnhetId(enhetId)
                .setHovedmal(hovedmal)
                .setInnsatsgruppe(innsatsgruppe);

        kafkaProducer.sendVedtakSendt(kafkaVedtakSendt);

        ConsumerRecord<String, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);

        assertNotNull(received);
        assertEquals(aktorId, received.key());
        assertEquals(kafkaTopics.getVedtakSendt(), received.topic());

        KafkaVedtakSendt receivedVedtak = JsonUtils.fromJson(received.value(), KafkaVedtakSendt.class);
        assertEquals(aktorId, receivedVedtak.getAktorId());
        assertEquals(id, receivedVedtak.getId());
        assertEquals(vedtakSendt, receivedVedtak.getVedtakSendt());
        assertEquals(enhetId, receivedVedtak.getEnhetId());
        assertEquals(hovedmal, receivedVedtak.getHovedmal());
        assertEquals(innsatsgruppe, receivedVedtak.getInnsatsgruppe());
    }

    @Test
    public void skal_produsere_vedtak_status_endring_melding() throws InterruptedException {
        String aktorId = "11111111";
        long vedtakId = 42;
        LocalDateTime timestamp = LocalDateTime.now();
        VedtakStatusEndring vedtakStatusEndring = VedtakStatusEndring.VEDTAK_SENDT;

        KafkaVedtakStatusEndring statuEndring = new KafkaVedtakStatusEndring()
                .setVedtakId(vedtakId)
                .setAktorId(aktorId)
                .setVedtakStatusEndring(vedtakStatusEndring)
                .setTimestamp(timestamp);

        kafkaProducer.sendVedtakStatusEndring(statuEndring);

        ConsumerRecord<String, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);

        assertNotNull(received);
        assertEquals(aktorId, received.key());
        assertEquals(kafkaTopics.getVedtakStatusEndring(), received.topic());

        KafkaVedtakStatusEndring receivedStatusEndring = JsonUtils.fromJson(received.value(), KafkaVedtakStatusEndring.class);
        assertEquals(aktorId, receivedStatusEndring.getAktorId());
        assertEquals(vedtakId, receivedStatusEndring.getVedtakId());
        assertEquals(timestamp, receivedStatusEndring.getTimestamp());
        assertEquals(vedtakStatusEndring, receivedStatusEndring.getVedtakStatusEndring());
    }

    @Test
    public void skal_produsere_utkast_opprettet_melding() throws InterruptedException {
        String aktorId = "11111111";
        String veilederIdent = "Z1234";
        String veilederNavn = "Test Testersen";

        KafkaVedtakStatusEndring utkastOpprettet = new KafkaVedtakStatusEndring.UtkastOpprettet()
                .setVeilederIdent(veilederIdent)
                .setVeilederNavn(veilederNavn)
                .setAktorId(aktorId);

        kafkaProducer.sendVedtakStatusEndring(utkastOpprettet);

        ConsumerRecord<String, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);

        assertNotNull(received);
        assertEquals(aktorId, received.key());
        assertEquals(kafkaTopics.getVedtakStatusEndring(), received.topic());

        KafkaVedtakStatusEndring.UtkastOpprettet receivedStatusEndring = JsonUtils.fromJson(received.value(), KafkaVedtakStatusEndring.UtkastOpprettet.class);
        assertEquals(aktorId, receivedStatusEndring.getAktorId());
        assertEquals(veilederIdent, receivedStatusEndring.getVeilederIdent());
        assertEquals(veilederNavn, receivedStatusEndring.getVeilederNavn());
        assertEquals(VedtakStatusEndring.UTKAST_OPPRETTET, receivedStatusEndring.getVedtakStatusEndring());
    }

    @Test
    public void skal_produsere_vedtak_sendt_melding() throws InterruptedException {
        String aktorId = "11111111";
        Innsatsgruppe innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS;
        Hovedmal hovedmal = Hovedmal.SKAFFE_ARBEID;

        KafkaVedtakStatusEndring vedtakSendt = new KafkaVedtakStatusEndring.VedtakSendt()
                .setHovedmal(hovedmal)
                .setInnsatsgruppe(innsatsgruppe)
                .setAktorId(aktorId);

        kafkaProducer.sendVedtakStatusEndring(vedtakSendt);

        ConsumerRecord<String, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);

        assertNotNull(received);
        assertEquals(aktorId, received.key());
        assertEquals(kafkaTopics.getVedtakStatusEndring(), received.topic());

        KafkaVedtakStatusEndring.VedtakSendt receivedStatusEndring = JsonUtils.fromJson(received.value(), KafkaVedtakStatusEndring.VedtakSendt.class);
        assertEquals(aktorId, receivedStatusEndring.getAktorId());
        assertEquals(innsatsgruppe, receivedStatusEndring.getInnsatsgruppe());
        assertEquals(hovedmal, receivedStatusEndring.getHovedmal());
        assertEquals(VedtakStatusEndring.VEDTAK_SENDT, receivedStatusEndring.getVedtakStatusEndring());
    }

    @Test
    public void skal_produsere_bli_beslutter_melding() throws InterruptedException {
        String aktorId = "11111111";
        String beslutterIdent = "Z1234";
        String beslutterNavn = "Test Testersen";

        KafkaVedtakStatusEndring bliBeslutter = new KafkaVedtakStatusEndring.BliBeslutter()
                .setBeslutterIdent(beslutterIdent)
                .setBeslutterNavn(beslutterNavn)
                .setAktorId(aktorId);

        kafkaProducer.sendVedtakStatusEndring(bliBeslutter);

        ConsumerRecord<String, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);

        assertNotNull(received);
        assertEquals(aktorId, received.key());
        assertEquals(kafkaTopics.getVedtakStatusEndring(), received.topic());

        KafkaVedtakStatusEndring.BliBeslutter receivedStatusEndring = JsonUtils.fromJson(received.value(), KafkaVedtakStatusEndring.BliBeslutter.class);
        assertEquals(aktorId, receivedStatusEndring.getAktorId());
        assertEquals(beslutterIdent, receivedStatusEndring.getBeslutterIdent());
        assertEquals(beslutterNavn, receivedStatusEndring.getBeslutterNavn());
        assertEquals(VedtakStatusEndring.BLI_BESLUTTER, receivedStatusEndring.getVedtakStatusEndring());
    }

    @Test
    public void skal_produsere_overta_for_beslutter_melding() throws InterruptedException {
        String aktorId = "11111111";
        String beslutterIdent = "Z1234";
        String beslutterNavn = "Test Testersen";

        KafkaVedtakStatusEndring bliBeslutter = new KafkaVedtakStatusEndring.OvertaForBeslutter()
                .setBeslutterIdent(beslutterIdent)
                .setBeslutterNavn(beslutterNavn)
                .setAktorId(aktorId);

        kafkaProducer.sendVedtakStatusEndring(bliBeslutter);

        ConsumerRecord<String, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);

        assertNotNull(received);
        assertEquals(aktorId, received.key());
        assertEquals(kafkaTopics.getVedtakStatusEndring(), received.topic());

        KafkaVedtakStatusEndring.OvertaForBeslutter receivedStatusEndring = JsonUtils.fromJson(received.value(), KafkaVedtakStatusEndring.OvertaForBeslutter.class);
        assertEquals(aktorId, receivedStatusEndring.getAktorId());
        assertEquals(beslutterIdent, receivedStatusEndring.getBeslutterIdent());
        assertEquals(beslutterNavn, receivedStatusEndring.getBeslutterNavn());
        assertEquals(VedtakStatusEndring.OVERTA_FOR_BESLUTTER, receivedStatusEndring.getVedtakStatusEndring());
    }

    @Test
    public void skal_produsere_overta_for_veileder_melding() throws InterruptedException {
        String aktorId = "11111111";
        String veilederIdent = "Z1234";
        String veilederNavn = "Test Testersen";

        KafkaVedtakStatusEndring overtaForVeileder = new KafkaVedtakStatusEndring.OvertaForVeileder()
                .setVeilederIdent(veilederIdent)
                .setVeilederNavn(veilederNavn)
                .setAktorId(aktorId);

        kafkaProducer.sendVedtakStatusEndring(overtaForVeileder);

        ConsumerRecord<String, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);

        assertNotNull(received);
        assertEquals(aktorId, received.key());
        assertEquals(kafkaTopics.getVedtakStatusEndring(), received.topic());

        KafkaVedtakStatusEndring.OvertaForVeileder receivedStatusEndring = JsonUtils.fromJson(received.value(), KafkaVedtakStatusEndring.OvertaForVeileder.class);
        assertEquals(aktorId, receivedStatusEndring.getAktorId());
        assertEquals(veilederIdent, receivedStatusEndring.getVeilederIdent());
        assertEquals(veilederNavn, receivedStatusEndring.getVeilederNavn());
        assertEquals(VedtakStatusEndring.OVERTA_FOR_VEILEDER, receivedStatusEndring.getVedtakStatusEndring());
    }

    @Test
    public void skal_produsere_tidligere_feilet_melding() throws InterruptedException {
        String aktorId = "11111111";
        String veilederIdent = "Z1234";
        String veilederNavn = "Test Testersen";

        KafkaVedtakStatusEndring overtaForVeileder = new KafkaVedtakStatusEndring.OvertaForVeileder()
                .setVeilederIdent(veilederIdent)
                .setVeilederNavn(veilederNavn)
                .setAktorId(aktorId);

        String messageJson = JsonUtils.toJson(overtaForVeileder);

        FeiletKafkaMelding feiletKafkaMelding = new FeiletKafkaMelding()
                .setId(42)
                .setJsonPayload(messageJson)
                .setKey(aktorId)
                .setOffset(1)
                .setTopic(KafkaTopics.Topic.VEDTAK_STATUS_ENDRING)
                .setType(MeldingType.PRODUCED);

        kafkaProducer.sendTidligereFeilet(feiletKafkaMelding);

        ConsumerRecord<String, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);

        assertNotNull(received);
        assertEquals(aktorId, received.key());
        assertEquals(kafkaTopics.getVedtakStatusEndring(), received.topic());

        KafkaVedtakStatusEndring.OvertaForVeileder receivedStatusEndring = JsonUtils.fromJson(received.value(), KafkaVedtakStatusEndring.OvertaForVeileder.class);
        assertEquals(aktorId, receivedStatusEndring.getAktorId());
        assertEquals(veilederIdent, receivedStatusEndring.getVeilederIdent());
        assertEquals(veilederNavn, receivedStatusEndring.getVeilederNavn());
        assertEquals(VedtakStatusEndring.OVERTA_FOR_VEILEDER, receivedStatusEndring.getVedtakStatusEndring());
    }

}

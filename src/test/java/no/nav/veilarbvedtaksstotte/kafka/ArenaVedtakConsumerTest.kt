package no.nav.veilarbvedtaksstotte.kafka

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig
import no.nav.veilarbvedtaksstotte.config.KafkaProperties
import no.nav.veilarbvedtaksstotte.domain.kafka.After
import no.nav.veilarbvedtaksstotte.domain.kafka.ArenaVedtakRecord
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaHovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.service.InnsatsbehovService
import no.nav.veilarbvedtaksstotte.service.KafkaConsumerService
import no.nav.veilarbvedtaksstotte.utils.TestUtils
import no.nav.veilarbvedtaksstotte.utils.toJson
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.KafkaContainer
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [ApplicationTestConfig::class])
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ArenaVedtakConsumerTest {

    @Autowired
    lateinit var kafkaContainer: KafkaContainer

    @Autowired
    lateinit var kafkaProperties: KafkaProperties

    lateinit var producer: KafkaProducer<String, String>

    @MockBean
    lateinit var innsatsbehovService: InnsatsbehovService

    @Autowired
    lateinit var kafkaConsumerService: KafkaConsumerService

    @Before
    fun setup() {
        producer = KafkaProducer(
            mapOf(
                Pair(BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers()),
                Pair(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java),
                Pair(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            )
        )
    }

    @org.junit.After
    fun after() {
        producer.close()
    }

    @Test
    fun `konsumerer melding med riktig format`() {
        val readTestResourceFile = TestUtils.readTestResourceFile("arena-vedtak-kafka-record.json")

        val forventetArenaVedtak = ArenaVedtak(
            fnr = Fnr("11111111111"),
            innsatsgruppe = ArenaInnsatsgruppe.BFORM,
            hovedmal = ArenaHovedmal.SKAFFEA,
            fraDato = LocalDate.of(2021, 1, 13),
            regUser = "REGUSER",
            operationTimestamp =
            LocalDateTime
                .of(2021, 2, 14, 15, 16, 17)
                .plus(12300, ChronoUnit.MICROS),
            hendelseId = 4321,
            vedtakId = 1234
        )

        producer.send(ProducerRecord(kafkaProperties.arenaVedtakTopic, "key", readTestResourceFile))

        TestUtils.verifiserAsynkront(
            10, TimeUnit.SECONDS
        ) {
            verify(innsatsbehovService).behandleEndringFraArena(forventetArenaVedtak)
        }
    }

    @Test
    fun `konsumerer melding med gydlige verdier`() {
        ArenaInnsatsgruppe.values().map { it.name }.forEach { innsatsgruppe ->
            ArenaHovedmal.values().map { it.name }.plus(null).forEach { hovedmal ->

                val forventetArenaVedtak = ArenaVedtak(
                    fnr = Fnr(randomNumeric(4)),
                    innsatsgruppe = ArenaInnsatsgruppe.valueOf(innsatsgruppe),
                    hovedmal = hovedmal?.let { ArenaHovedmal.valueOf(it) },
                    fraDato = LocalDate.now(),
                    regUser = "reguser",
                    operationTimestamp = LocalDateTime.now(),
                    hendelseId = 1234,
                    vedtakId = 1
                )
                val arenaVedtakRecord = arenaVedtakRecordMed(
                    fnr = forventetArenaVedtak.fnr,
                    kvalifiseringsgruppe = innsatsgruppe,
                    hovedmal = hovedmal,
                    opTs = forventetArenaVedtak.operationTimestamp,
                    fraDato = forventetArenaVedtak.fraDato,
                    regUser = forventetArenaVedtak.regUser,
                    hendelseId = forventetArenaVedtak.hendelseId,
                    vedtakId = forventetArenaVedtak.vedtakId
                )

                producer.send(ProducerRecord(kafkaProperties.arenaVedtakTopic, "key", arenaVedtakRecord.toJson()))

                TestUtils.verifiserAsynkront(
                    10, TimeUnit.SECONDS
                ) {
                    verify(innsatsbehovService).behandleEndringFraArena(forventetArenaVedtak)
                }
            }
        }
    }

    @Test
    fun `konsumerer ikke melding der kvalifiseringsgruppe ikke er en innsatsgruppe`() {
        val arenaVedtakRecord = ArenaVedtakRecord(
            table = "table",
            opType = "I",
            opTs = LocalDateTime.now().toString().replace('T', ' '),
            currentTs = LocalDateTime.now().toString(),
            pos = "1",
            after = After(
                vedtakId = 321,
                fnr = "1",
                kvalifiseringsgruppe = "VURDI",
                hovedmal = ArenaHovedmal.SKAFFEA.name,
                regUser = "reguser",
                fraDato = "${LocalDate.now()} 00:00:00",
                hendelseId = 123
            )
        )

        kafkaConsumerService.behandleArenaVedtak(kafkaRecord(arenaVedtakRecord))

        verify(innsatsbehovService, never()).behandleEndringFraArena(any())
    }

    @Test
    fun `konsumerer ikke melding der hovedmal ikke er kjent`() {
        val arenaVedtakRecord = arenaVedtakRecordMed(
            fnr = Fnr("1"),
            kvalifiseringsgruppe = ArenaInnsatsgruppe.BATT.name,
            hovedmal = "FEIL",
        )

        kafkaConsumerService.behandleArenaVedtak(kafkaRecord(arenaVedtakRecord))

        verify(innsatsbehovService, never()).behandleEndringFraArena(any())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `feiler dersom fraDato mangler`() {
        val arenaVedtakRecord = arenaVedtakRecordMed(
            fnr = Fnr("1"),
            kvalifiseringsgruppe = ArenaInnsatsgruppe.BATT.name,
            hovedmal = ArenaHovedmal.BEHOLDEA.name,
            fraDato = null
        )

        kafkaConsumerService.behandleArenaVedtak(kafkaRecord(arenaVedtakRecord))

        verify(innsatsbehovService, never()).behandleEndringFraArena(any())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `feiler dersom regUser mangler`() {
        val arenaVedtakRecord = arenaVedtakRecordMed(
            fnr = Fnr("1"),
            kvalifiseringsgruppe = ArenaInnsatsgruppe.BATT.name,
            hovedmal = ArenaHovedmal.BEHOLDEA.name,
            regUser = null
        )

        kafkaConsumerService.behandleArenaVedtak(kafkaRecord(arenaVedtakRecord))

        verify(innsatsbehovService, never()).behandleEndringFraArena(any())
    }

    private fun <T> any(): T = Mockito.any()

    private fun arenaVedtakRecordMed(
        fnr: Fnr,
        kvalifiseringsgruppe: String,
        hovedmal: String? = null,
        opTs: LocalDateTime = LocalDateTime.now(),
        currentTs: LocalDateTime = LocalDateTime.now(),
        fraDato: LocalDate? = LocalDate.now(),
        regUser: String? = "default reg user",
        hendelseId: Long = 123,
        vedtakId: Long = 321
    ): ArenaVedtakRecord {
        return ArenaVedtakRecord(
            table = "table",
            opType = "I",
            opTs = opTs.toString().replace('T', ' '),
            currentTs = currentTs.toString(),
            pos = "1",
            after = After(
                vedtakId = vedtakId,
                fraDato = fraDato?.let { "$it 00:00:00" },
                regUser = regUser,
                fnr = fnr.get(),
                kvalifiseringsgruppe = kvalifiseringsgruppe,
                hovedmal = hovedmal,
                hendelseId = hendelseId
            )
        )
    }

    private fun <V> kafkaRecord(value: V) = ConsumerRecord("", 0, 0, "", value)
}

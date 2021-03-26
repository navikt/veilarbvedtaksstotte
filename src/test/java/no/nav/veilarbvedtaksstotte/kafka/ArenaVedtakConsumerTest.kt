package no.nav.veilarbvedtaksstotte.kafka

import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.ConsumerUtils.jsonConsumer
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig
import no.nav.veilarbvedtaksstotte.domain.kafka.After
import no.nav.veilarbvedtaksstotte.domain.kafka.ArenaVedtakRecord
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaHovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.service.InnsatsbehovService
import no.nav.veilarbvedtaksstotte.service.KafkaConsumerService
import no.nav.veilarbvedtaksstotte.utils.JsonUtils
import no.nav.veilarbvedtaksstotte.utils.TestUtils
import no.nav.veilarbvedtaksstotte.utils.TestUtils.KAFKA_IMAGE
import no.nav.veilarbvedtaksstotte.utils.toJson
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.List
import java.util.Map
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [ApplicationTestConfig::class])
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ArenaVedtakConsumerTest {

    companion object {
        private val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse(KAFKA_IMAGE))

        @ClassRule
        fun getKafka() = kafka
    }

    val topicName = "topic-name"

    lateinit var producer: KafkaProducer<String, String>
    lateinit var consumer: KafkaConsumerClient<String, String>

    @MockBean
    lateinit var innsatsbehovService: InnsatsbehovService

    lateinit var kafkaConsumerService: KafkaConsumerService

    @Before
    fun setup() {

        JsonUtils.init()

        getKafka().start()
        val brokerUrl = getKafka().getBootstrapServers()

        kafkaConsumerService = KafkaConsumerService(null, innsatsbehovService)

        val admin = KafkaAdminClient.create(Map.of<String, Any>(BOOTSTRAP_SERVERS_CONFIG, brokerUrl))

        admin.createTopics(
            List.of(
                NewTopic(topicName, 1, 1.toShort())
            )
        )

        admin.close()

        producer = KafkaProducer(
            mapOf(
                Pair(BOOTSTRAP_SERVERS_CONFIG, brokerUrl),
                Pair(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java),
                Pair(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            )
        )

        val consumerProps = Properties()
        consumerProps.put(BOOTSTRAP_SERVERS_CONFIG, brokerUrl)
        consumerProps.put(ENABLE_AUTO_COMMIT_CONFIG, false)
        consumerProps.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        consumerProps.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        consumerProps.put(GROUP_ID_CONFIG, "test-consumer")
        consumerProps.put(ENABLE_AUTO_COMMIT_CONFIG, false)
        consumerProps.put(MAX_POLL_RECORDS_CONFIG, 5 * 60 * 1000)
        consumerProps.put(AUTO_OFFSET_RESET_CONFIG, "earliest")

        consumer = KafkaConsumerClientBuilder
            .builder<String, String>()
            .withConsumer(
                topicName,
                jsonConsumer(
                    ArenaVedtakRecord::class.java,
                    Consumer { record -> kafkaConsumerService.behandleArenaVedtak(record) })
            )
            .withProps(consumerProps)
            .build()

        consumer.start()
    }

    @org.junit.After
    fun after() {
        producer.close()
        consumer.stop()
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
        )

        producer.send(ProducerRecord(topicName, "key", readTestResourceFile))

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
                    fnr = Fnr("fnr"),
                    innsatsgruppe = ArenaInnsatsgruppe.valueOf(innsatsgruppe),
                    hovedmal = hovedmal?.let { ArenaHovedmal.valueOf(it) },
                    fraDato = LocalDate.now(),
                    regUser = "reguser",
                    operationTimestamp = LocalDateTime.now(),
                )

                val arenaVedtakRecord = ArenaVedtakRecord(
                    table = "table",
                    opType = "I",
                    opTs = forventetArenaVedtak.operationTimestamp.toString().replace('T', ' '),
                    currentTs = LocalDateTime.now().toString(),
                    pos = "1",
                    after = After(
                        fnr = forventetArenaVedtak.fnr.get(),
                        kvalifiseringsgruppe = innsatsgruppe,
                        hovedmal = hovedmal,
                        regUser = forventetArenaVedtak.regUser,
                        fraDato = "${forventetArenaVedtak.fraDato} 00:00:00"
                    )
                )

                producer.send(ProducerRecord(topicName, "key", arenaVedtakRecord.toJson()))


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
                fnr = "1",
                kvalifiseringsgruppe = "VURDI",
                hovedmal = ArenaHovedmal.SKAFFEA.name,
                regUser = "reguser",
                fraDato = "${LocalDate.now()} 00:00:00"
            )
        )

        kafkaConsumerService.behandleArenaVedtak(arenaVedtakRecord)

        verify(innsatsbehovService, never()).behandleEndringFraArena(any())
    }

    @Test
    fun `konsumerer ikke melding der hovedmal ikke er kjent`() {
        val arenaVedtakRecord = ArenaVedtakRecord(
            table = "table",
            opType = "I",
            opTs = LocalDateTime.now().toString().replace('T', ' '),
            currentTs = LocalDateTime.now().toString(),
            pos = "1",
            after = After(
                fnr = "1",
                kvalifiseringsgruppe = ArenaInnsatsgruppe.BATT.name,
                hovedmal = "FEIL",
                regUser = "reguser",
                fraDato = "${LocalDate.now()} 00:00:00"
            )
        )

        kafkaConsumerService.behandleArenaVedtak(arenaVedtakRecord)

        verify(innsatsbehovService, never()).behandleEndringFraArena(any())
    }

    private fun <T> any(): T = Mockito.any()
}

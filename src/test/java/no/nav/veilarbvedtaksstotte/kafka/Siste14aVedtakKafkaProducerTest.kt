package no.nav.veilarbvedtaksstotte.kafka

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.kafka.consumer.ConsumeStatus
import no.nav.common.kafka.consumer.TopicConsumer
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak.HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.service.Siste14aVedtakService
import no.nav.veilarbvedtaksstotte.service.KafkaProducerService
import no.nav.veilarbvedtaksstotte.utils.TestUtils
import no.nav.veilarbvedtaksstotte.utils.TimeUtils
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.KafkaContainer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.set

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [ApplicationTestConfig::class])
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class Siste14aVedtakKafkaProducerTest {

    @Autowired
    lateinit var kafkaContainer: KafkaContainer

    @Autowired
    lateinit var siste14aVedtakService: Siste14aVedtakService

    @MockBean
    lateinit var aktorOppslagClient: AktorOppslagClient

    @MockBean
    lateinit var veilarboppfolgingClient: VeilarboppfolgingClient

    @Autowired
    lateinit var kafkaProducerService: KafkaProducerService

    @Test
    fun `produserer melding for siste 14a vedtak basert på nytt vedtak`() {
        val aktorId = AktorId(randomNumeric(10))
        val arenaVedtak = ArenaVedtak(
            fnr = Fnr(randomNumeric(10)),
            innsatsgruppe = ArenaVedtak.ArenaInnsatsgruppe.BFORM,
            hovedmal = ArenaVedtak.ArenaHovedmal.SKAFFEA,
            fraDato = LocalDate.of(2021, 1, 13),
            regUser = "REGUSER",
            operationTimestamp =
            LocalDateTime
                .of(2021, 2, 14, 15, 16, 17)
                .plus(12300, ChronoUnit.MICROS),
            hendelseId = 4321,
            vedtakId = 1234
        )

        `when`(aktorOppslagClient.hentIdenter(arenaVedtak.fnr)).thenReturn(
            BrukerIdenter(arenaVedtak.fnr, aktorId, emptyList(), emptyList())
        )

        `when`(veilarboppfolgingClient.hentOppfolgingsperioder(arenaVedtak.fnr.get())).thenReturn(
            listOf(
                OppfolgingPeriodeDTO(ZonedDateTime.of(2021, 1, 11, 2, 1, 0, 0, ZoneId.systemDefault()), null)
            )
        )

        siste14aVedtakService.behandleEndringFraArena(arenaVedtak)

        val konsumerteMeldinger: AtomicReference<MutableMap<AktorId, Siste14aVedtak?>> = AtomicReference(mutableMapOf())

        KafkaConsumerClientBuilder.builder()
            .withProperties(kafkaTestConsumerProperties(kafkaContainer.bootstrapServers))
            .withTopicConfig(
                KafkaConsumerClientBuilder.TopicConfig<String, Siste14aVedtak>()
                    .withConsumerConfig(
                        "siste14aVedtakTopic",
                        Deserializers.stringDeserializer(),
                        Deserializers.jsonDeserializer(Siste14aVedtak::class.java),
                        TopicConsumer { record ->
                            konsumerteMeldinger.get().set(AktorId(record.key()), record.value())
                            ConsumeStatus.OK
                        }
                    )
            )
            .build()
            .start()

        TestUtils.verifiserAsynkront(10, TimeUnit.SECONDS) {
            val konsumertMelding = konsumerteMeldinger.get()[aktorId]

            assertNotNull(konsumertMelding)
            assertEquals(aktorId, konsumertMelding!!.aktorId)
            assertEquals(Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, konsumertMelding.innsatsgruppe)
            assertEquals(SKAFFE_ARBEID, konsumertMelding.hovedmal)
            assertEquals(arenaVedtak.fraDato, TimeUtils.toLocalDate(konsumertMelding.fattetDato))
            assertTrue(konsumertMelding.fraArena)
        }
    }

    fun kafkaTestConsumerProperties(brokerUrl: String?): Properties {
        val props = Properties()
        props[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = brokerUrl
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.java
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        props[ConsumerConfig.GROUP_ID_CONFIG] = "test-consumer"
        props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 5 * 60 * 1000
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        return props
    }
}

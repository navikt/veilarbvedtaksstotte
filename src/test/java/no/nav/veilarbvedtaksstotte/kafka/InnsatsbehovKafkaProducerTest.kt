package no.nav.veilarbvedtaksstotte.kafka

import no.nav.common.kafka.consumer.ConsumeStatus
import no.nav.common.kafka.consumer.util.JsonTopicConsumer
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig
import no.nav.veilarbvedtaksstotte.domain.BrukerIdenter
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov.HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.service.BrukerIdentService
import no.nav.veilarbvedtaksstotte.service.InnsatsbehovService
import no.nav.veilarbvedtaksstotte.service.KafkaProducerService
import no.nav.veilarbvedtaksstotte.utils.TestUtils
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
class InnsatsbehovKafkaProducerTest {

    @Autowired
    lateinit var kafkaContainer: KafkaContainer

    @Autowired
    lateinit var innsatsbehovService: InnsatsbehovService

    @MockBean
    lateinit var brukerIdentService: BrukerIdentService

    @MockBean
    lateinit var veilarboppfolgingClient: VeilarboppfolgingClient

    @Autowired
    lateinit var kafkaProducerService: KafkaProducerService

    @Test
    fun `produserer melding for endring av innsatsbehov med nytt innsatsbehov`() {
        val aktorId = AktorId("123123")
        val arenaVedtak = ArenaVedtak(
            fnr = Fnr("11111111111"),
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

        `when`(brukerIdentService.hentIdenter(arenaVedtak.fnr)).thenReturn(
            BrukerIdenter(arenaVedtak.fnr, aktorId, emptyList(), emptyList())
        )

        `when`(veilarboppfolgingClient.hentOppfolgingsperioder(arenaVedtak.fnr.get())).thenReturn(
            listOf(
                OppfolgingPeriodeDTO(ZonedDateTime.of(2021, 1, 11, 2, 1, 0, 0, ZoneId.systemDefault()), null)
            )
        )

        innsatsbehovService.behandleEndringFraArena(arenaVedtak)

        val konsumertMelding: AtomicReference<MutableMap<AktorId, Innsatsbehov?>> = AtomicReference(mutableMapOf())

        KafkaConsumerClientBuilder.builder<String, String>()
            .withProperties(kafkaTestConsumerProperties(kafkaContainer.bootstrapServers))
            .withConsumer(
                /*kafkaProperties.innsatsbehovTopic*/"innsatsbehovTopic",
                JsonTopicConsumer(Innsatsbehov::class.java) { record, innsatsbehov: Innsatsbehov? ->
                    konsumertMelding.get().set(AktorId(record.key()), innsatsbehov)
                    ConsumeStatus.OK
                }
            ).build().start()

        TestUtils.verifiserAsynkront(10, TimeUnit.SECONDS) {
            assertEquals(
                Innsatsbehov(aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, SKAFFE_ARBEID),
                konsumertMelding.get()[aktorId]
            )
        }
    }

    fun kafkaTestConsumerProperties(brokerUrl: String?): Properties {
        val props = Properties()
        props[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = brokerUrl
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        props[ConsumerConfig.GROUP_ID_CONFIG] = "test-consumer"
        props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 5 * 60 * 1000
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        return props
    }
}

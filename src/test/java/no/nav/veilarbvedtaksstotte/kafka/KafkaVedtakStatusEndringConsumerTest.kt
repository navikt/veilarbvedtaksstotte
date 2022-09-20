package no.nav.veilarbvedtaksstotte.kafka

import com.ninjasquad.springmockk.MockkBean
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.mockk.every
import no.nav.common.types.identer.AktorId
import no.nav.pto_schema.kafka.avro.Vedtak14aFattetDvh
import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig
import no.nav.veilarbvedtaksstotte.config.KafkaConfig
import no.nav.veilarbvedtaksstotte.config.KafkaProperties
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.UnleashService
import no.nav.veilarbvedtaksstotte.utils.KafkaTestUtils
import no.nav.veilarbvedtaksstotte.utils.TestData
import no.nav.veilarbvedtaksstotte.utils.TestUtils
import no.nav.veilarbvedtaksstotte.utils.toJson
import org.apache.commons.lang3.RandomStringUtils
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.KafkaContainer
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@SpringBootTest(classes = [ApplicationTestConfig::class])
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class KafkaVedtakStatusEndringConsumerTest {

    @Autowired
    lateinit var kafkaProperties: KafkaProperties

    @Autowired
    lateinit var kafkaContainer: KafkaContainer

    @Autowired
    lateinit var testProducer: KafkaTestProducer

    @Autowired
    lateinit var kafkaAvroContext: KafkaConfig.KafkaAvroContext

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var vedtakRepository: VedtaksstotteRepository

    @MockkBean
    lateinit var unleashService: UnleashService

    @Test
    fun `konsumerer melding om statusendring som fører til publisering av melding pa topic for rapportering til DVH`() {

        every {
            unleashService.isRapporterDvhAsynkront
        } returns true

        val vedtak = gittFattetVedtakDer(
            aktorId = AktorId.of(RandomStringUtils.randomNumeric(10)),
            vedtakFattetDato = LocalDateTime.now()
        )

        val statusEndring = KafkaVedtakStatusEndring.VedtakSendt()
        statusEndring.setInnsatsgruppe(vedtak.innsatsgruppe)
            .setHovedmal(vedtak.hovedmal)
            .setTimestamp(LocalDateTime.now())
            .setVedtakId(1)
            .setAktorId(vedtak.aktorId)


        testProducer.send(
            ProducerRecord(kafkaProperties.vedtakStatusEndringTopic, "key", statusEndring.toJson())
        )

        val konsumerteMeldinger: AtomicReference<MutableMap<String, Vedtak14aFattetDvh>> =
            AtomicReference(mutableMapOf())

        val testConsumer = KafkaTestUtils.testConsumer<Any>(
            brokerUrl = kafkaContainer.bootstrapServers,
            topicName = kafkaProperties.vedtakFattetDvhTopic,
            valueDeserializer = KafkaAvroDeserializer(null, kafkaAvroContext.config)
        ) { record ->
            konsumerteMeldinger.get()[record.key()] = record.value() as Vedtak14aFattetDvh
        }

        testConsumer.start()

        TestUtils.verifiserAsynkront(
            10, TimeUnit.SECONDS
        ) {
            val konsumertMelding = konsumerteMeldinger.get()[vedtak.aktorId]

            assertNotNull(konsumertMelding)
        }
    }

    private fun gittFattetVedtakDer(
        aktorId: AktorId,
        vedtakFattetDato: LocalDateTime
    ): Vedtak {
        vedtakRepository.opprettUtkast(
            aktorId.get(),
            TestData.TEST_VEILEDER_IDENT,
            TestData.TEST_OPPFOLGINGSENHET_ID
        )
        val vedtak = vedtakRepository.hentUtkast(aktorId.get())
        vedtak.innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS
        vedtak.hovedmal = Hovedmal.BEHOLDE_ARBEID
        vedtakRepository.oppdaterUtkast(vedtak.id, vedtak)
        vedtakRepository.ferdigstillVedtak(vedtak.id)
        jdbcTemplate.update(
            "UPDATE VEDTAK SET VEDTAK_FATTET = ? WHERE ID = ?",
            vedtakFattetDato,
            vedtak.id
        )

        return vedtakRepository.hentVedtak(vedtak.id)
    }
}

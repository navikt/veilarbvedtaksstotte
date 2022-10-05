package no.nav.veilarbvedtaksstotte.kafka

import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.config.KafkaProperties
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak.HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
import no.nav.veilarbvedtaksstotte.service.Siste14aVedtakService
import no.nav.veilarbvedtaksstotte.utils.*
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.testcontainers.containers.KafkaContainer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.set

class Siste14aVedtakKafkaProducerTest : AbstractVedtakIntegrationTest() {

    @Autowired
    lateinit var kafkaProperties: KafkaProperties

    @Autowired
    lateinit var kafkaContainer: KafkaContainer

    @Autowired
    lateinit var siste14aVedtakService: Siste14aVedtakService

    @MockBean
    lateinit var veilarboppfolgingClient: VeilarboppfolgingClient

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

        val konsumerteMeldinger: AtomicReference<MutableMap<AktorId, Siste14aVedtak>> = AtomicReference(mutableMapOf())

        val testConsumer = KafkaTestUtils.testConsumer<Siste14aVedtak>(
            kafkaContainer.bootstrapServers,
            kafkaProperties.siste14aVedtakTopic,
        ) { record ->
            konsumerteMeldinger.get()[AktorId(record.key())] = record.value()
        }

        testConsumer.start()

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
}

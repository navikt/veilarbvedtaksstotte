package no.nav.veilarbvedtaksstotte.service

import io.getunleash.DefaultUnleash
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage
import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.IntegrationTestBase
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakSendt
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring
import no.nav.veilarbvedtaksstotte.domain.kafka.VedtakStatusEndring
import no.nav.veilarbvedtaksstotte.domain.vedtak.Gjeldende14aVedtakKafkaDTO
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal.BEHOLDE_ARBEID
import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe.SPESIELT_TILPASSET_INNSATS
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe.STANDARD_INNSATS
import no.nav.veilarbvedtaksstotte.domain.vedtak.InnsatsgruppeV2
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.utils.JsonUtils
import no.nav.veilarbvedtaksstotte.utils.PRODUSER_OBO_GJELDENDE_14A_VEDTAK_MELDINGER_SKRUDD_PAA
import no.nav.veilarbvedtaksstotte.utils.TestData.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.LocalDateTime
import java.time.ZonedDateTime

class KafkaProducerServiceTest : IntegrationTestBase() {
    @MockBean
    lateinit var producerRecordStorage: KafkaProducerRecordStorage

    @Captor
    lateinit var argumentCaptor: ArgumentCaptor<ProducerRecord<ByteArray, ByteArray>>

    @Autowired
    lateinit var kafkaProducerService: KafkaProducerService

    @Autowired
    lateinit var unleashService: DefaultUnleash

    @Test
    fun `lagrer forventet record verdi for oppdatering av siste vedtak`() {
        val siste14aVedtak = Siste14aVedtak(
            aktorId = AktorId(TEST_AKTOR_ID),
            innsatsgruppe = STANDARD_INNSATS,
            hovedmal = SKAFFE_ARBEID,
            fattetDato = ZonedDateTime.parse("2021-09-08T09:29:20.398043+02:00"),
            fraArena = false
        )

        kafkaProducerService.sendSiste14aVedtak(siste14aVedtak)

        verify(producerRecordStorage).store(argumentCaptor.capture())

        val forventet = """
            {
              "aktorId": "123",
              "innsatsgruppe": "STANDARD_INNSATS",
              "hovedmal": "SKAFFE_ARBEID",
              "fattetDato": "2021-09-08T09:29:20.398043+02:00",
              "fraArena": false
            }
        """

        assertEqualJson(forventet, deserialize(argumentCaptor.value.value()))
    }

    @Test
    fun `lagrer forventet record verdi for sending av vedtak`() {
        val kafkaVedtakSendt = KafkaVedtakSendt()
        kafkaVedtakSendt.setId(123)
        kafkaVedtakSendt.setVedtakSendt(LocalDateTime.of(2021, 4, 7, 11, 12, 32, 1234))
        kafkaVedtakSendt.setInnsatsgruppe(SPESIELT_TILPASSET_INNSATS)
        kafkaVedtakSendt.setHovedmal(BEHOLDE_ARBEID)
        kafkaVedtakSendt.setAktorId(TEST_AKTOR_ID)
        kafkaVedtakSendt.setEnhetId(TEST_OPPFOLGINGSENHET_ID)

        kafkaProducerService.sendVedtakSendt(kafkaVedtakSendt)

        verify(producerRecordStorage).store(argumentCaptor.capture())

        val forventet = """
            {
              "id": 123,
              "vedtakSendt": "2021-04-07T11:12:32.000001234+02:00",
              "innsatsgruppe": "SPESIELT_TILPASSET_INNSATS",
              "hovedmal": "BEHOLDE_ARBEID",
              "aktorId": "123",
              "enhetId": "1234"
            }
        """

        assertEqualJson(forventet, deserialize(argumentCaptor.value.value()))
    }

    @Test
    fun `lagrer forventet record verdi for statusendring på vedtak`() {
        val utkastOpprettet = KafkaVedtakStatusEndring.UtkastOpprettet()
        utkastOpprettet.setVedtakId(23442)
        utkastOpprettet.setAktorId(TEST_AKTOR_ID)
        utkastOpprettet.setVedtakStatusEndring(VedtakStatusEndring.UTKAST_OPPRETTET)
        utkastOpprettet.setTimestamp(LocalDateTime.of(2021, 4, 5, 9, 16, 44, 345534))
        utkastOpprettet.setVeilederIdent(TEST_VEILEDER_IDENT)
        utkastOpprettet.setVeilederNavn(TEST_VEILEDER_NAVN)

        kafkaProducerService.sendVedtakStatusEndring(utkastOpprettet)

        verify(producerRecordStorage).store(argumentCaptor.capture())

        val forventet = """
            {
              "vedtakId": 23442,
              "aktorId": "123",
              "vedtakStatusEndring": "UTKAST_OPPRETTET",
              "timestamp": "2021-04-05T09:16:44.000345534+02:00",
              "veilederIdent": "Z123456",
              "veilederNavn": "Veileder Veilederen"
            }
        """

        assertEqualJson(forventet, deserialize(argumentCaptor.value.value()))
    }

    @Test
    fun `lagrer forventet record verdi for gjeldende § 14 a-vedtak`() {
        // Given
        `when`(unleashService.isEnabled(PRODUSER_OBO_GJELDENDE_14A_VEDTAK_MELDINGER_SKRUDD_PAA)).thenReturn(true)
        val aktorId = AktorId("11111111111")
        val gjeldende14aVedtakKafkaDTO = Gjeldende14aVedtakKafkaDTO(
            aktorId = aktorId,
            innsatsgruppe = InnsatsgruppeV2.GODE_MULIGHETER,
            hovedmal = HovedmalMedOkeDeltakelse.fraHovedmal(BEHOLDE_ARBEID)
        )

        // When
        kafkaProducerService.sendGjeldende14aVedtak(aktorId, gjeldende14aVedtakKafkaDTO)

        // Then
        verify(producerRecordStorage).store(argumentCaptor.capture())

        //language=JSON
        val forventet = """
            {
              "aktorId": "11111111111",
              "innsatsgruppe": "GODE_MULIGHETER",
              "hovedmal": "BEHOLDE_ARBEID"
            }
        """.trimIndent()

        assertEqualJson(forventet, deserialize(argumentCaptor.value.value()))
    }

    fun assertEqualJson(expexted: String, actual: String) {
        assertEquals(JsonUtils.objectMapper.readTree(expexted), JsonUtils.objectMapper.readTree(actual))
    }

    fun deserialize(bytes: ByteArray): String {
        val deserializer = stringDeserializer()
        return deserializer.deserialize("", bytes)
    }
}

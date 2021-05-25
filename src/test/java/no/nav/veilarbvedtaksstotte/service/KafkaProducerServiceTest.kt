package no.nav.veilarbvedtaksstotte.service

import org.junit.Assert.assertEquals
import no.nav.common.json.JsonUtils
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage
import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.config.KafkaProperties
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakSendt
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring
import no.nav.veilarbvedtaksstotte.domain.kafka.VedtakStatusEndring
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal.BEHOLDE_ARBEID
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov.HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe.SPESIELT_TILPASSET_INNSATS
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe.STANDARD_INNSATS
import no.nav.veilarbvedtaksstotte.utils.TestData.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.time.LocalDateTime

class KafkaProducerServiceTest {

    @Mock
    lateinit var producerRecordStorage: KafkaProducerRecordStorage<String, String>

    @Captor
    lateinit var argumentCaptor: ArgumentCaptor<ProducerRecord<String, String>>

    lateinit var kafkaProducerService: KafkaProducerService

    lateinit var kafkaProperties: KafkaProperties

    @Before
    fun setup() {
        kafkaProperties = KafkaProperties()
        kafkaProperties.innsatsbehovTopic = "innsatsbehovTopic"
        kafkaProperties.vedtakSendtTopic = "vedtakSendtTopic"
        kafkaProperties.vedtakStatusEndringTopic = "vedtakStatusEndringTopic"
        MockitoAnnotations.initMocks(this);
        kafkaProducerService = KafkaProducerService(producerRecordStorage, kafkaProperties)
    }

    @Test
    fun `lagrer forventet record verdi for oppdatering av innsatsbehov`() {
        kafkaProducerService.sendInnsatsbehov(
            Innsatsbehov(
                aktorId = AktorId(TEST_AKTOR_ID),
                innsatsgruppe = STANDARD_INNSATS,
                hovedmal = SKAFFE_ARBEID
            )
        )

        verify(producerRecordStorage).store(argumentCaptor.capture())

        val forventet = """
            {
              "aktorId": "123",
              "innsatsgruppe": "STANDARD_INNSATS",
              "hovedmal": "SKAFFE_ARBEID"
            }
        """

        assertEqualJson(forventet, argumentCaptor.value.value())
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

        assertEqualJson(forventet, argumentCaptor.value.value())
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

        assertEqualJson(forventet, argumentCaptor.value.value())
    }

    fun assertEqualJson(expexted: String, actual: String) {
        assertEquals(JsonUtils.getMapper().readTree(expexted), JsonUtils.getMapper().readTree(actual))
    }
}

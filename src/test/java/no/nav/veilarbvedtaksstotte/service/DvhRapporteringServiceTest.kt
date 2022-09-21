package no.nav.veilarbvedtaksstotte.service

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage
import no.nav.common.types.identer.AktorId
import no.nav.pto_schema.kafka.avro.Vedtak14aFattetDvh
import no.nav.pto_schema.kafka.avro.Vedtak14aFattetDvhHovedmalKode
import no.nav.pto_schema.kafka.avro.Vedtak14aFattetDvhInnsatsgruppeKode
import no.nav.veilarbvedtaksstotte.config.KafkaConfig
import no.nav.veilarbvedtaksstotte.config.KafkaProperties
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.utils.AbstractVedtakIntegrationTest
import no.nav.veilarbvedtaksstotte.utils.TestData
import no.nav.veilarbvedtaksstotte.utils.TimeUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.LocalDateTime

class DvhRapporteringServiceTest : AbstractVedtakIntegrationTest() {

    @Autowired
    lateinit var kafkaAvroContext: KafkaConfig.KafkaAvroContext

    @Autowired
    lateinit var dvhRapporteringService: DvhRapporteringService

    @Autowired
    lateinit var kafkaProperties: KafkaProperties

    @MockBean
    lateinit var producerRecordStorage: KafkaProducerRecordStorage

    @Captor
    lateinit var argumentCaptor: ArgumentCaptor<ProducerRecord<ByteArray, ByteArray>>

    @Test
    fun `lagrer forventet record verdi for sending av vedtak til dvh basert på statusendring vedtak sendt`() {

        val vedtak = lagreFattetVedtak(
            aktorId = AktorId(RandomStringUtils.randomNumeric(10)),
            innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
            hovedmal = Hovedmal.SKAFFE_ARBEID,
            vedtakFattetDato = LocalDateTime.of(2021, 4, 7, 11, 12, 32, 1234),
            enhetId = TestData.TEST_OPPFOLGINGSENHET_ID,
            veilederIdent = "324",
            beslutterIdent = "678"
        )

        val forventetVedtak14aFattetDvh = Vedtak14aFattetDvh()
        forventetVedtak14aFattetDvh.id = vedtak.id
        forventetVedtak14aFattetDvh.vedtakFattet = TimeUtils.toInstant(LocalDateTime.of(2021, 4, 7, 11, 12, 32, 1234))
        forventetVedtak14aFattetDvh.innsatsgruppeKode = Vedtak14aFattetDvhInnsatsgruppeKode.SITUASJONSBESTEMT_INNSATS
        forventetVedtak14aFattetDvh.hovedmalKode = Vedtak14aFattetDvhHovedmalKode.SKAFFE_ARBEID
        forventetVedtak14aFattetDvh.aktorId = vedtak.aktorId
        forventetVedtak14aFattetDvh.oppfolgingsenhetId = TestData.TEST_OPPFOLGINGSENHET_ID
        forventetVedtak14aFattetDvh.veilederIdent = "324"
        forventetVedtak14aFattetDvh.beslutterIdent = "678"

        dvhRapporteringService.rapporterTilDvh(KafkaVedtakStatusEndring.VedtakSendt().setVedtakId(vedtak.id))

        Mockito.verify(producerRecordStorage).store(argumentCaptor.capture())

        val deserializer = KafkaAvroDeserializer(null, kafkaAvroContext.config)

        val resultat = deserializer
            .deserialize(kafkaProperties.vedtakFattetDvhTopic, argumentCaptor.value.value()) as Vedtak14aFattetDvh

        Assertions.assertEquals(forventetVedtak14aFattetDvh, resultat)
    }

}

package no.nav.veilarbvedtaksstotte.kafka

import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.config.KafkaProperties
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring
import no.nav.veilarbvedtaksstotte.service.KafkaVedtakStatusEndringConsumer
import no.nav.veilarbvedtaksstotte.utils.AbstractVedtakIntegrationTest
import no.nav.veilarbvedtaksstotte.utils.TestUtils
import no.nav.veilarbvedtaksstotte.utils.TestUtils.randomNumeric
import no.nav.veilarbvedtaksstotte.utils.toJson
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class KafkaVedtakStatusEndringConsumerTest : AbstractVedtakIntegrationTest() {

    @Autowired
    lateinit var kafkaProperties: KafkaProperties

    @Autowired
    lateinit var testProducer: KafkaTestProducer
    
    @SpykBean
    lateinit var kafkaVedtakStatusEndringConsumer: KafkaVedtakStatusEndringConsumer

    @Test
    fun `konsumerer melding om statusendring`() {

        val vedtak = lagreFattetVedtak(
            aktorId = AktorId.of(randomNumeric(10)),
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

        TestUtils.verifiserAsynkront(
            10, TimeUnit.SECONDS
        ) {
            verify {
                kafkaVedtakStatusEndringConsumer.konsumer(match {
                    it.value() == KafkaVedtakStatusEndring()
                        .setVedtakId(statusEndring.vedtakId)
                        .setAktorId(statusEndring.aktorId)
                        .setVedtakStatusEndring(statusEndring.vedtakStatusEndring)
                        .setTimestamp(statusEndring.timestamp)
                })
            }
        }
    }
}

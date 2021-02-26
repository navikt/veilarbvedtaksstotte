package no.nav.veilarbvedtaksstotte.kafka

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.service.ArenaVedtakService
import no.nav.veilarbvedtaksstotte.utils.JsonUtils
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_AKTOR_ID
import no.nav.veilarbvedtaksstotte.utils.TestUtils.verifiserAsynkront
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [ApplicationTestConfig::class])
class ArenaVedtakConsumerTest {

    @SpyBean
    lateinit var arenaVedtakService: ArenaVedtakService

    @SpyBean
    lateinit var kafkaProducer: KafkaProducer

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    lateinit var kafkaTopics: KafkaTopics

    @Test
    fun `meldinger for vedtak fra arena blir konsumert og innsatsbehov blir sendt på kafka`() {
        val arenaVedtak = ArenaVedtak(
            fnr = Fnr(RandomStringUtils.randomNumeric(10)),
            innsatsgruppe = ArenaVedtak.ArenaInnsatsgruppe.BATT,
            hovedmal = ArenaVedtak.ArenaHovedmal.SKAFFE_ARBEID,
            fraDato = LocalDateTime.now(),
            regUser = "reg user"
        )

        kafkaTemplate.send(kafkaTopics.arenaVedtak, JsonUtils.objectMapper.writeValueAsString(arenaVedtak))

        verifiserAsynkront(10, TimeUnit.SECONDS) {
            verify(arenaVedtakService).behandleVedtakFraArena(eq(arenaVedtak))
            verify(kafkaProducer).sendInnsatsbehov(eq(
                Innsatsbehov(
                    aktorId = AktorId(TEST_AKTOR_ID),
                    innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                    hovedmal = Innsatsbehov.HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
                )))
        }
    }
}

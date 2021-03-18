package no.nav.veilarbvedtaksstotte.kafka

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaHovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.kafka.dto.After
import no.nav.veilarbvedtaksstotte.kafka.dto.ArenaVedtakRecord
import no.nav.veilarbvedtaksstotte.service.InnsatsbehovService
import no.nav.veilarbvedtaksstotte.utils.TestUtils
import no.nav.veilarbvedtaksstotte.utils.toJson
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [ApplicationTestConfig::class])
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ArenaVedtakConsumerTest {

    @MockBean
    lateinit var innsatsbehovService: InnsatsbehovService

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    lateinit var kafkaTopics: KafkaTopics

    @Autowired
    lateinit var kafkaConsumer: KafkaConsumer

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

        kafkaTemplate.send(kafkaTopics.arenaVedtak, readTestResourceFile)

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

                kafkaTemplate.send(kafkaTopics.arenaVedtak, arenaVedtakRecord.toJson())


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

        kafkaConsumer.consumeArenaVedtak(
            consumerRecord(arenaVedtakRecord.toJson()),
            Mockito.mock(Acknowledgment::class.java)
        )

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

        kafkaConsumer.consumeArenaVedtak(
            consumerRecord(arenaVedtakRecord.toJson()),
            Mockito.mock(Acknowledgment::class.java)
        )

        verify(innsatsbehovService, never()).behandleEndringFraArena(any())
    }

    private fun consumerRecord(json: String): ConsumerRecord<String, String> {
        return ConsumerRecord("", 0, 0, "", json)
    }

    private fun <T> any(): T = Mockito.any()
}

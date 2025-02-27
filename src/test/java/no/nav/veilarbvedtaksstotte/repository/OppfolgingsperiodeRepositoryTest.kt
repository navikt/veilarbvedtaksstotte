package no.nav.veilarbvedtaksstotte.repository

import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsperiode
import no.nav.veilarbvedtaksstotte.domain.kafka.StartetBegrunnelse
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.ZonedDateTime
import java.util.*

class OppfolgingsperiodeRepositoryTest : DatabaseTest() {

    companion object {
        lateinit var oppfolgingsperiodeRepository: OppfolgingsperiodeRepository

        @BeforeAll
        @JvmStatic
        fun setup() {
            oppfolgingsperiodeRepository = OppfolgingsperiodeRepository(jdbcTemplate)
        }
    }

    @Test
    fun `lagre oppfolgingsperiode`() {

        val oppfolgingsperiodeFraKafka = KafkaOppfolgingsperiode(
            uuid = UUID.fromString("955b2735-e824-4f2a-b703-76658760a4cc"),
            aktorId = "2228184718032",
            ZonedDateTime.now(),
            null,
            StartetBegrunnelse.ARBEIDSSOKER
        )

        assertDoesNotThrow { oppfolgingsperiodeRepository.insertOppfolgingsperiode(oppfolgingsperiodeFraKafka) }

    }
}
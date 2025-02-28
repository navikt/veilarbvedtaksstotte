package no.nav.veilarbvedtaksstotte.repository

import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaSisteOppfolgingsperiode
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.ZonedDateTime
import java.util.*

class SisteOppfolgingPeriodeRepositoryTest : DatabaseTest() {

    companion object {
        lateinit var sisteOppfolgingPeriodeRepository: SisteOppfolgingPeriodeRepository

        @BeforeAll
        @JvmStatic
        fun setup() {
            sisteOppfolgingPeriodeRepository = SisteOppfolgingPeriodeRepository(jdbcTemplate)
        }
    }

    @Test
    fun `lagre siste oppfolgingsperiode`() {

        val oppfolgingsperiodeFraKafka = KafkaSisteOppfolgingsperiode(
            uuid = UUID.fromString("955b2735-e824-4f2a-b703-76658760a4cc"),
            aktorId = "2228184718032",
            ZonedDateTime.now(),
            null,
        )

        assertDoesNotThrow { sisteOppfolgingPeriodeRepository.upsertSisteOppfolgingPeriode(oppfolgingsperiodeFraKafka) }

    }
}

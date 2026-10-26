package no.nav.veilarbvedtaksstotte.repository

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.oppfolgingsperiode.SisteOppfolgingsperiode
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNull
import java.time.ZoneId
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
        assertDoesNotThrow {
            sisteOppfolgingPeriodeRepository.upsertSisteOppfolgingPeriode(
                UUID.fromString("955b2735-e824-4f2a-b703-76658760a4cc"),
                "2228184718032",
                ZonedDateTime.now(),
                null,
            )
        }
    }

    @Test
    fun `hente inneværende oppfolgingsperiode når bruker er under oppfølging`() {
        // given
        val uuid = UUID.fromString("955b2735-e824-4f2a-b703-76658760a4cc")
        val aktorId = AktorId.of("2228184718032")
        val startTidspunkt = ZonedDateTime.of(2025, 3, 14, 15, 9, 26, 0, ZoneId.systemDefault())
        val sluttTidspunkt = null

        sisteOppfolgingPeriodeRepository.upsertSisteOppfolgingPeriode(
            uuid,
            aktorId.toString(),
            startTidspunkt,
            sluttTidspunkt,
        )

        // when
        val innevarendeOppfolgingsperiode = sisteOppfolgingPeriodeRepository.hentInnevaerendeOppfolgingsperiode(aktorId)

        // then
        val forventetOppfolgingsperiode = SisteOppfolgingsperiode(
            oppfolgingsperiodeId = uuid,
            aktorId = aktorId,
            startdato = startTidspunkt,
            sluttdato = sluttTidspunkt
        )

        assertNotNull(innevarendeOppfolgingsperiode)
        assertEquals(forventetOppfolgingsperiode, innevarendeOppfolgingsperiode)
    }

    @Test
    fun `ikkje finne oppfølgingsperiode når bruker ikkje er under oppfølging`() {
        // given
        val uuid = UUID.fromString("955b2735-e824-4f2a-b703-76658760a4cc")
        val aktorId = AktorId.of("2228184718032")
        val startTidspunkt = ZonedDateTime.now().minusDays(1)
        val sluttTidspunkt = ZonedDateTime.now()

        sisteOppfolgingPeriodeRepository.upsertSisteOppfolgingPeriode(
            uuid,
            aktorId.toString(),
            startTidspunkt,
            sluttTidspunkt,
        )

        // when
        val innevarendeOppfolgingsperiode = sisteOppfolgingPeriodeRepository.hentInnevaerendeOppfolgingsperiode(aktorId)

        // then
        assertNull(innevarendeOppfolgingsperiode)
    }

    @Test
    fun `ignorer EmptyResultDataAccessException når oppfølgingsperiode ikke fins`() {
        // given
        val aktorId = AktorId.of("2228184718032")
        jdbcTemplate.update(
            "DELETE FROM SISTE_OPPFOLGING_PERIODE WHERE AKTORID = ?", aktorId.get()
        )

        val logger = secureLog.teamLog as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)

        try {
            val resultat = sisteOppfolgingPeriodeRepository.hentInnevaerendeOppfolgingsperiode(aktorId)
            assertNull(resultat)

            val feilmeldinger = appender.list.filter {
                it.level == Level.ERROR &&
                        it.formattedMessage.startsWith("Kunne ikke hente siste oppfolgingsperiode for aktorId: ")
            }
            assertTrue(feilmeldinger.isEmpty())
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }
}


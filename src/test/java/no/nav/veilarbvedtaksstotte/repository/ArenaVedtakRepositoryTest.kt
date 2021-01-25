package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

class ArenaVedtakRepositoryTest {

    companion object {
        lateinit var jdbcTemplate: JdbcTemplate
        lateinit var arenaVedtakRepository: ArenaVedtakRepository

        @BeforeClass
        @JvmStatic
        fun setup() {
            jdbcTemplate = SingletonPostgresContainer.init().db
            arenaVedtakRepository = ArenaVedtakRepository(jdbcTemplate)
        }
    }

    @Test
    fun `lagre, oppdatere og hente arena-vedtak`() {
        val fnr = Fnr(randomNumeric(10))
        val forventetOpprinneligVedtak = ArenaVedtak(
            fnr = fnr,
            innsatsgruppe = ArenaVedtak.ArenaInnsatsgruppe.BATT,
            hovedmal = ArenaVedtak.ArenaHovedmal.SKAFFE_ARBEID,
            fraDato = LocalDateTime.now(),
            modUser = "mod user"
        )

        arenaVedtakRepository.upsertVedtak(forventetOpprinneligVedtak)

        val lagretVedtak = arenaVedtakRepository.hentVedtak(fnr)

        assertEquals(forventetOpprinneligVedtak, lagretVedtak)

        val forventetOppdatertVedtak = forventetOpprinneligVedtak
            .copy(
                innsatsgruppe = ArenaVedtak.ArenaInnsatsgruppe.VARIG,
                hovedmal = ArenaVedtak.ArenaHovedmal.OKE_DELTAKELSE,
                fraDato = forventetOpprinneligVedtak.fraDato.plusDays(1),
                modUser = "mod user 2"
            )

        arenaVedtakRepository.upsertVedtak(forventetOppdatertVedtak)

        val lagretOppdatertVedtak = arenaVedtakRepository.hentVedtak(fnr)

        assertEquals(forventetOppdatertVedtak, lagretOppdatertVedtak)
    }

    @Test
    fun `upsert oppdaterer forventet arena-vedtak`() {
        val arenaVedtak1 = ArenaVedtak(
            fnr = Fnr(randomNumeric(10)),
            innsatsgruppe = ArenaVedtak.ArenaInnsatsgruppe.BATT,
            hovedmal = ArenaVedtak.ArenaHovedmal.SKAFFE_ARBEID,
            fraDato = LocalDateTime.now(),
            modUser = "mod user"
        )
        val arenaVedtak2 = arenaVedtak1.copy(fnr = Fnr(randomNumeric(10)))

        arenaVedtakRepository.upsertVedtak(arenaVedtak1)
        arenaVedtakRepository.upsertVedtak(arenaVedtak2)

        assertEquals(arenaVedtak1, arenaVedtakRepository.hentVedtak(arenaVedtak1.fnr))
        assertEquals(arenaVedtak2, arenaVedtakRepository.hentVedtak(arenaVedtak2.fnr))

        val oppdatertArenaVedtak2 = arenaVedtak2.copy(fraDato = arenaVedtak2.fraDato.plusDays(2))
        arenaVedtakRepository.upsertVedtak(oppdatertArenaVedtak2)

        assertEquals(arenaVedtak1, arenaVedtakRepository.hentVedtak(arenaVedtak1.fnr))
        assertEquals(oppdatertArenaVedtak2, arenaVedtakRepository.hentVedtak(arenaVedtak2.fnr))
    }

    @Test
    fun `sletter arena-vedtak basert på liste av fnr`() {
        val arenaVedtak1 = ArenaVedtak(
            fnr = Fnr(randomNumeric(10)),
            innsatsgruppe = ArenaVedtak.ArenaInnsatsgruppe.BATT,
            hovedmal = ArenaVedtak.ArenaHovedmal.SKAFFE_ARBEID,
            fraDato = LocalDateTime.now(),
            modUser = "mod user"
        )
        val arenaVedtak2 = arenaVedtak1.copy(fnr = Fnr(randomNumeric(10)))
        val arenaVedtak3 = arenaVedtak1.copy(fnr = Fnr(randomNumeric(10)))

        arenaVedtakRepository.upsertVedtak(arenaVedtak1)
        arenaVedtakRepository.upsertVedtak(arenaVedtak2)
        arenaVedtakRepository.upsertVedtak(arenaVedtak3)

        assertNotNull(arenaVedtakRepository.hentVedtak(arenaVedtak1.fnr))
        assertNotNull(arenaVedtakRepository.hentVedtak(arenaVedtak2.fnr))
        assertNotNull(arenaVedtakRepository.hentVedtak(arenaVedtak3.fnr))

        val antall = arenaVedtakRepository.slettVedtak(listOf(arenaVedtak1.fnr, arenaVedtak3.fnr))

        assertEquals(2, antall)

        assertNull(arenaVedtakRepository.hentVedtak(arenaVedtak1.fnr))
        assertNotNull(arenaVedtakRepository.hentVedtak(arenaVedtak2.fnr))
        assertNull(arenaVedtakRepository.hentVedtak(arenaVedtak3.fnr))
    }
}

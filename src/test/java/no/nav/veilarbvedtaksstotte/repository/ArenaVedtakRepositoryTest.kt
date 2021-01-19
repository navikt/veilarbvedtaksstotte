package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR
import org.junit.Assert
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
    fun `lagre og hente arena-vedtak`() {
        val vedtak = ArenaVedtak(
            fnr = Fnr(TEST_FNR),
            innsatsgruppe = ArenaVedtak.ArenaInnsatsgruppe.BATT,
            hovedmal = ArenaVedtak.ArenaHovedmal.SKAFFE_ARBEID,
            fraDato = LocalDateTime.now(),
            modUser = "mod user"
        )

        arenaVedtakRepository.insertVedtak(vedtak)

        val hentVedtak = arenaVedtakRepository.hentVedtak(vedtak.fnr)

        Assert.assertEquals(vedtak, hentVedtak)
    }
}

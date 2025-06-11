package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.*

class TestvedtakRepositoryTest: DatabaseTest() {

    companion object {
        lateinit var testvedtakRepository: TestvedtakRepository
        private var mock = Mockito.mockStatic(EnvironmentUtils::class.java)

        @BeforeAll
        @JvmStatic
        fun setup() {
            testvedtakRepository = TestvedtakRepository(jdbcTemplate)
            mock.`when`<Optional<Boolean>> { EnvironmentUtils.isDevelopment() }.thenReturn(Optional.of(true))
        }

        @AfterAll
        @JvmStatic
        fun closedown() {
            DbTestUtils.cleanupDb(jdbcTemplate)
            mock.close()
        }
    }

    @Test
    fun `lagre og hent testvedtak`() {
        val aktorId = AktorId.of("12345678910")
        val testVedtak = Vedtak()
            .settAktorId(aktorId.get())
            .settHovedmal(Hovedmal.SKAFFE_ARBEID)
            .settInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS)
            .settOppfolgingsenhetId("0220")
            .settUtkastOpprettet(LocalDateTime.now().minusDays(1))
            .settVedtakFattet(LocalDateTime.now().minusDays(1))
            .settUtkastSistOppdatert(LocalDateTime.now().minusDays(1))
            .settReferanse(UUID.randomUUID())
            .settVeilederIdent("Z123456")


        testvedtakRepository.lagreTestvedtak(testVedtak, Fnr.of("12345678910").get())

        val hentetVedtak = testvedtakRepository.hentTestvedtak(aktorId)
        assertNotNull(hentetVedtak)
        assertEquals(testVedtak.aktorId, hentetVedtak?.aktorId)
        assertEquals(testVedtak.innsatsgruppe, hentetVedtak?.innsatsgruppe)
    }


}
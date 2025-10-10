package no.nav.veilarbvedtaksstotte.repository

import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RetryVedtakdistribusjonRepositoryTest : DatabaseTest() {
    companion object {
        lateinit var retryVedtakdistribusjonRepository: RetryVedtakdistribusjonRepository
        lateinit var vedtaksstotteRepository: VedtaksstotteRepository

        @BeforeAll
        @JvmStatic
        fun setupOnce() {
            vedtaksstotteRepository = VedtaksstotteRepository(jdbcTemplate, transactor)
            retryVedtakdistribusjonRepository = RetryVedtakdistribusjonRepository(jdbcTemplate)
        }
    }

    @BeforeEach
    fun setup() {
        DbTestUtils.cleanupDb(jdbcTemplate)

        val aktorId = "1234567890"
        vedtaksstotteRepository.opprettUtkast(aktorId, "Z123456", "007")
        val vedtak = vedtaksstotteRepository.hentUtkast(aktorId)
        vedtaksstotteRepository.lagreJournalforingVedtak(vedtak.id, "JP1", "dok123")
    }


    @Test
    fun `skal opprette en rad med DISTRIBUSJONSFORSOK = 1`() {
        retryVedtakdistribusjonRepository.insertJournalpostIdEllerOkMedEn("JP1")

        val count = jdbcTemplate.queryForObject(
            "SELECT DISTRIBUSJONSFORSOK FROM RETRY_VEDTAKDISTRIBUSJON WHERE JOURNALPOST_ID = ?",
            Int::class.java,
            "JP1"
        )

        assertEquals(count, 1)
    }

    @Test
    fun `skal øke DISTRIBUSJONSFORSOK hvis raden eksisterer`() {
        retryVedtakdistribusjonRepository.insertJournalpostIdEllerOkMedEn("JP1")
        // Call again to increment
        retryVedtakdistribusjonRepository.insertJournalpostIdEllerOkMedEn("JP1")

        val count = jdbcTemplate.queryForObject(
            "SELECT DISTRIBUSJONSFORSOK FROM RETRY_VEDTAKDISTRIBUSJON WHERE JOURNALPOST_ID = ?",
            Int::class.java,
            "JP1"
        )

        assertEquals(count, 2)
    }
}

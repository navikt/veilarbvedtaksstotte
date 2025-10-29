package no.nav.veilarbvedtaksstotte.repository

import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val journalpostId1 = "JP1"

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
        vedtaksstotteRepository.lagreJournalforingVedtak(vedtak.id, journalpostId1, "dok123")
    }

    private fun hentAntallDistribusjonsforsok(journalpostId: String): Int {
        return jdbcTemplate.queryForObject(
            // MAX sørger for at vi får null hvis raden ikke finnes og COALESCE returner første ikke-null-verdi, altså 0
            "SELECT COALESCE(MAX(DISTRIBUSJONSFORSOK), 0) FROM RETRY_VEDTAKDISTRIBUSJON WHERE JOURNALPOST_ID = ?",
            Int::class.java,
            journalpostId
        )
    }

    @Test
    fun `skal opprette en rad med DISTRIBUSJONSFORSOK = 1`() {
        retryVedtakdistribusjonRepository.insertJournalpostIdEllerInkrementerAntallRetriesMedEn(journalpostId1)

        val count = hentAntallDistribusjonsforsok(journalpostId1)

        assertEquals(1, count)
    }

    @Test
    fun `skal øke DISTRIBUSJONSFORSOK hvis raden eksisterer`() {
        retryVedtakdistribusjonRepository.insertJournalpostIdEllerInkrementerAntallRetriesMedEn(journalpostId1)
        // Kall igjen for å øke antall forsøk
        retryVedtakdistribusjonRepository.insertJournalpostIdEllerInkrementerAntallRetriesMedEn(journalpostId1)

        val count = hentAntallDistribusjonsforsok(journalpostId1)

        assertEquals(2, count)
    }

    @Test
    fun `skal slette raden for en gitt journalpostId`() {
        retryVedtakdistribusjonRepository.insertJournalpostIdEllerInkrementerAntallRetriesMedEn(journalpostId1)

        val beforeDelete = hentAntallDistribusjonsforsok(journalpostId1)
        assertEquals(1, beforeDelete)

        retryVedtakdistribusjonRepository.deleteJournalpostId(journalpostId1)

        val afterDelete = hentAntallDistribusjonsforsok(journalpostId1)
        assertEquals(0, afterDelete)
    }

}

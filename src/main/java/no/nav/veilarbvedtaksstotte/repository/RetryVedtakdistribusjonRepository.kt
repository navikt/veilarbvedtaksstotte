package no.nav.veilarbvedtaksstotte.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class RetryVedtakdistribusjonRepository(val jdbcTemplate: JdbcTemplate) {
    companion object {
        const val RETRY_VEDTAKDISTRIBUSJON_TABELL = "RETRY_VEDTAKDISTRIBUSJON"
    }

    fun insertJournalpostIdEllerOkMedEn(journalpostId: String) {
        val sql = """
            INSERT INTO $RETRY_VEDTAKDISTRIBUSJON_TABELL (journalpost_id)
            VALUES (?)
            ON CONFLICT (journalpost_id) DO UPDATE 
            SET DISTRIBUSJONSFORSOK = RETRY_VEDTAKDISTRIBUSJON.DISTRIBUSJONSFORSOK + 1
        """.trimIndent()
        jdbcTemplate.update(sql, journalpostId)
    }

    fun deleteJournalpostId(journalpostId: String) {
        val sql = "DELETE FROM $RETRY_VEDTAKDISTRIBUSJON_TABELL WHERE journalpost_id = ?"
        jdbcTemplate.update(sql, journalpostId)
    }
}

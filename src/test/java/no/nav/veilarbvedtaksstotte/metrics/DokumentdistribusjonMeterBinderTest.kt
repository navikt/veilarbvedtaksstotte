package no.nav.veilarbvedtaksstotte.metrics

import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class DokumentdistribusjonMeterBinderTest : DatabaseTest() {
    lateinit var vedtaksstotteRepository: VedtaksstotteRepository
    lateinit var dokumentdistribusjonMeterBinder: DokumentdistribusjonMeterBinder

    @Before
    fun setup() {
        vedtaksstotteRepository = VedtaksstotteRepository(jdbcTemplate, transactor)
        dokumentdistribusjonMeterBinder = DokumentdistribusjonMeterBinder(vedtaksstotteRepository)
    }

    @Test
    fun `finner antall journalførte vedtak som ikke er distribuert fra en tid tilbake, frem til nå men med litt delay`() {
        val now = LocalDateTime.now()

        // for kort tid siden:
        lagreVedtak(now, erDistribuert = true)
        lagreVedtak(now, erDistribuert = false)

        // innenfor:
        lagreVedtak(now.minusSeconds(65), erDistribuert = true)
        lagreVedtak(now.minusSeconds(65), erDistribuert = false)
        lagreVedtak(now.minusDays(2), erDistribuert = true)
        lagreVedtak(now.minusDays(2), erDistribuert = false)

        //for lenge siden:
        lagreVedtak(now.minusDays(3).minusMinutes(1), erDistribuert = true)
        lagreVedtak(now.minusDays(3).minusMinutes(1), erDistribuert = false)

        val antallJournalforteVedtakSomIkkeErDistribuert =
            dokumentdistribusjonMeterBinder.antallJournalforteVedtakUtenDokumentbestilling(fra = now.minusDays(3))

        assertEquals(2, antallJournalforteVedtakSomIkkeErDistribuert)
    }

    private fun lagreVedtak(fattetDato: LocalDateTime, erDistribuert: Boolean) {
        val aktorId = randomNumeric(10)
        val journalpostId = randomNumeric(10)
        val dokumentBestillingId = if (erDistribuert) randomNumeric(10) else null

        jdbcTemplate.update(
            """
            INSERT INTO VEDTAK(
                AKTOR_ID, VEILEDER_IDENT, OPPFOLGINGSENHET_ID, STATUS, UTKAST_SIST_OPPDATERT, VEDTAK_FATTET,
                JOURNALPOST_ID, DOKUMENT_BESTILLING_ID
            ) 
            VALUES(?, ?, ?, ?, ?, ?, ?, ?)
        """, aktorId, "VEILEDER", "1234", VedtakStatus.SENDT.name, fattetDato, fattetDato,
            journalpostId, dokumentBestillingId
        )
    }
}

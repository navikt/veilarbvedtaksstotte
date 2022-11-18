package no.nav.veilarbvedtaksstotte.metrics

import no.nav.veilarbvedtaksstotte.domain.DistribusjonBestillingId
import no.nav.veilarbvedtaksstotte.domain.DistribusjonBestillingId.Uuid
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils.cleanupDb
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DokumentdistribusjonMeterBinderTest : DatabaseTest() {
    lateinit var vedtaksstotteRepository: VedtaksstotteRepository
    lateinit var dokumentdistribusjonMeterBinder: DokumentdistribusjonMeterBinder

    @BeforeEach
    fun setup() {
        vedtaksstotteRepository = VedtaksstotteRepository(jdbcTemplate, transactor)
        dokumentdistribusjonMeterBinder = DokumentdistribusjonMeterBinder(vedtaksstotteRepository)
        cleanupDb(jdbcTemplate)
    }

    @Test
    fun `finner antall journalførte vedtak som ikke er distribuert frem til nå men med litt delay`() {
        val now = LocalDateTime.now()

        // for kort tid siden:
        lagreVedtak(now, dokumentBestillingId = Uuid(randomNumeric(10)))
        lagreVedtak(now, dokumentBestillingId = null)
        lagreVedtak(now.minusMinutes(13).plusSeconds(1), dokumentBestillingId = null)

        // innenfor:
        lagreVedtak(now.minusMinutes(13).minusSeconds(5), dokumentBestillingId = Uuid(randomNumeric(10)))
        lagreVedtak(now.minusMinutes(13).minusSeconds(5), dokumentBestillingId = null)
        lagreVedtak(now.minusDays(2), dokumentBestillingId = Uuid(randomNumeric(10)))
        lagreVedtak(now.minusDays(2), dokumentBestillingId = null)

        val antallJournalforteVedtakSomIkkeErDistribuert =
            dokumentdistribusjonMeterBinder.antallJournalforteVedtakUtenDokumentbestilling()

        assertEquals(2, antallJournalforteVedtakSomIkkeErDistribuert)
    }

    @Test
    fun `finner antall vedtak som har feilende distribusjon`() {
        val now = LocalDateTime.now()

        lagreVedtak(now, dokumentBestillingId = Uuid(randomNumeric(10)))
        lagreVedtak(now, dokumentBestillingId = DistribusjonBestillingId.Feilet)
        lagreVedtak(now.minusSeconds(125), dokumentBestillingId = Uuid(randomNumeric(10)))
        lagreVedtak(now.minusSeconds(125), dokumentBestillingId = DistribusjonBestillingId.Feilet)
        lagreVedtak(now.minusSeconds(125), dokumentBestillingId = null)
        lagreVedtak(now.minusDays(2), dokumentBestillingId = Uuid(randomNumeric(10)))
        lagreVedtak(now.minusDays(2), dokumentBestillingId = DistribusjonBestillingId.Feilet)

        val antallJournalforteVedtakSomIkkeErDistribuert =
            dokumentdistribusjonMeterBinder.antallJournalforteVedtakMedFeiletDokumentbestilling()

        assertEquals(3, antallJournalforteVedtakSomIkkeErDistribuert)
    }


    private fun lagreVedtak(fattetDato: LocalDateTime, dokumentBestillingId: DistribusjonBestillingId?) {
        val aktorId = randomNumeric(10)
        val journalpostId = randomNumeric(10)

        jdbcTemplate.update(
            """
            INSERT INTO VEDTAK(
                AKTOR_ID, VEILEDER_IDENT, OPPFOLGINGSENHET_ID, STATUS, UTKAST_SIST_OPPDATERT, VEDTAK_FATTET,
                JOURNALPOST_ID, DOKUMENT_BESTILLING_ID
            ) 
            VALUES(?, ?, ?, ?, ?, ?, ?, ?)
        """,
            aktorId,
            "VEILEDER",
            "1234",
            VedtakStatus.SENDT.name,
            fattetDato,
            fattetDato,
            journalpostId,
            dokumentBestillingId?.id
        )
    }
}

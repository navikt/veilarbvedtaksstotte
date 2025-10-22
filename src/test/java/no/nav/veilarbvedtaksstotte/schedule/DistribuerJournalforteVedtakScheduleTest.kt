package no.nav.veilarbvedtaksstotte.schedule

import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.dto.DistribuerJournalpostResponsDTO
import no.nav.veilarbvedtaksstotte.client.dokdistkanal.DokdistkanalClient
import no.nav.veilarbvedtaksstotte.domain.DistribusjonBestillingId
import no.nav.veilarbvedtaksstotte.domain.DistribusjonBestillingId.Feilet
import no.nav.veilarbvedtaksstotte.domain.DistribusjonBestillingId.Mangler
import no.nav.veilarbvedtaksstotte.domain.DistribusjonBestillingId.Uuid
import no.nav.veilarbvedtaksstotte.repository.RetryVedtakdistribusjonRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.DistribusjonService
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils.cleanupDb
import no.nav.veilarbvedtaksstotte.utils.TestUtils.randomAlphabetic
import no.nav.veilarbvedtaksstotte.utils.TestUtils.randomNumeric
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.time.LocalDateTime.now

class DistribuerJournalforteVedtakScheduleTest : DatabaseTest() {

    companion object {

        lateinit var leaderElection: LeaderElectionClient
        lateinit var dokdistribusjonClient: DokdistribusjonClient
        lateinit var dokdistkanalClient: DokdistkanalClient
        lateinit var distribusjonService: DistribusjonService
        lateinit var vedtakRepository: VedtaksstotteRepository
        lateinit var retryVedtakdistribusjonRepository: RetryVedtakdistribusjonRepository
        lateinit var distribuerJournalforteVedtakSchedule: DistribuerJournalforteVedtakSchedule

        @BeforeAll
        @JvmStatic
        fun setup() {
            leaderElection = mock(LeaderElectionClient::class.java)
            dokdistribusjonClient = mock(DokdistribusjonClient::class.java)
            vedtakRepository = VedtaksstotteRepository(jdbcTemplate, transactor)
            retryVedtakdistribusjonRepository = RetryVedtakdistribusjonRepository(jdbcTemplate)
            dokdistkanalClient = mock(DokdistkanalClient::class.java)
            distribusjonService = DistribusjonService(
                vedtakRepository,
                retryVedtakdistribusjonRepository,
                dokdistribusjonClient,
                dokdistkanalClient
            )
            distribuerJournalforteVedtakSchedule = DistribuerJournalforteVedtakSchedule(
                leaderElection = leaderElection,
                distribusjonService = distribusjonService,
                vedtaksstotteRepository = vedtakRepository,
            )
        }
    }

    @BeforeEach
    fun beforeEach() {
        cleanupDb(jdbcTemplate)
    }

    @Test
    fun `henter vedtak som skal distribueres, og distribuerer i batcher med avgrenset størrelse`() {
        `when`(leaderElection.isLeader).thenReturn(true)
        `when`(dokdistribusjonClient.distribuerJournalpost(any()))
            .then { DistribuerJournalpostResponsDTO(randomAlphabetic(10)) }

        val batchStorrelse = 10

        gittFlereVedtakSomIkkeSkalDistribueres()

        val distribueresForst = (1..batchStorrelse).toList().map {
            gittVedtakDer(
                vedtakFattetDato = now().minusMonths(1).plusDays(it.toLong()),
                dokumentBestillingId = null,
                journalpostId = randomNumeric(10)
            )
        }

        val distribueresAndre = (1..3).toList().map {
            gittVedtakDer(
                vedtakFattetDato = now().plusDays(it.toLong()),
                dokumentBestillingId = null,
                journalpostId = randomNumeric(10)
            )
        }


        DistribuerJournalforteVedtakSchedule.batchSize = batchStorrelse

        distribuerJournalforteVedtakSchedule.distribuerJournalforteVedtak()
        distribueresForst.forEach { assertDistribuert(it) }

        distribuerJournalforteVedtakSchedule.distribuerJournalforteVedtak()
        distribueresAndre.forEach { assertDistribuert(it) }

        verify(dokdistribusjonClient, times(distribueresForst.size + distribueresAndre.size))
            .distribuerJournalpost(any())

        reset(dokdistribusjonClient)
        distribuerJournalforteVedtakSchedule.distribuerJournalforteVedtak()
        verify(dokdistribusjonClient, never()).distribuerJournalpost(any())
    }

    @Test
    fun `henter feilende vedtak som skal forsøkes distribuert på nytt`() {
        `when`(leaderElection.isLeader).thenReturn(true)
        `when`(dokdistribusjonClient.distribuerJournalpost(any()))
            .then { DistribuerJournalpostResponsDTO(randomAlphabetic(10)) }

        val feilendeVedtak = (1..5).toList().map {
            gittFeilendeVedtakDer(
                vedtakFattetDato = now().minusMonths(1).plusDays(it.toLong()),
                dokumentBestillingId = null,
                journalpostId = randomNumeric(10),
                distribusjonsforsok = 9 + it // 10, 11, 12, 13, 14
            )
        }

        DistribuerJournalforteVedtakSchedule.batchSize = 10

        distribuerJournalforteVedtakSchedule.distribuerJournalforteFeilendeVedtak()
        feilendeVedtak.forEach { assertDistribuert(it) }

        // Kun de tre vedtakene med 12 eller flere feilede forsøk skal forsøkes på nytt
        verify(dokdistribusjonClient, times(3))
            .distribuerJournalpost(any())
    }

    fun gittFlereVedtakSomIkkeSkalDistribueres() {
        gittVedtakDer(
            vedtakFattetDato = null,
            dokumentBestillingId = null,
            journalpostId = null
        )
        gittVedtakDer(
            vedtakFattetDato = null,
            dokumentBestillingId = Uuid(randomAlphabetic(10)),
            journalpostId = randomAlphabetic(10)
        )
        gittVedtakDer(
            vedtakFattetDato = null,
            dokumentBestillingId = Feilet,
            journalpostId = randomAlphabetic(10)
        )
        gittVedtakDer(
            vedtakFattetDato = null,
            dokumentBestillingId = Mangler,
            journalpostId = randomAlphabetic(10)
        )
        gittVedtakDer(
            vedtakFattetDato = now().minusMinutes(10),
            dokumentBestillingId = Uuid(randomAlphabetic(10)),
            journalpostId = randomAlphabetic(10)
        )
        gittVedtakDer(
            vedtakFattetDato = now().minusMinutes(10),
            dokumentBestillingId = Feilet,
            journalpostId = randomAlphabetic(10)
        )
        gittVedtakDer(
            vedtakFattetDato = now().minusMinutes(10),
            dokumentBestillingId = Mangler,
            journalpostId = randomAlphabetic(10)
        )
        gittVedtakDer(
            vedtakFattetDato = null,
            dokumentBestillingId = Uuid(randomAlphabetic(10)),
            journalpostId = null
        )
        gittVedtakDer(
            vedtakFattetDato = now().minusMinutes(10),
            dokumentBestillingId = Uuid(randomAlphabetic(10)),
            journalpostId = null
        )
    }

    fun assertDistribuert(vedtakId: Long) {
        assertNotNull("Vedtak skal være distribuert", vedtakRepository.hentVedtak(vedtakId).dokumentbestillingId)
    }

    private fun gittVedtakDer(
        vedtakFattetDato: LocalDateTime?,
        dokumentBestillingId: DistribusjonBestillingId?,
        aktorId: AktorId = AktorId(randomNumeric(10)),
        veilederIdent: String = randomAlphabetic(1) + randomNumeric(6),
        oppfolgingsenhet: String = randomNumeric(4),
        journalpostId: String? = randomNumeric(10),
        dokumentId: String = randomNumeric(9)
    ): Long {
        vedtakRepository.opprettUtkast(
            aktorId.get(),
            veilederIdent,
            oppfolgingsenhet
        )
        val vedtak = vedtakRepository.hentUtkast(aktorId.get())
        vedtakRepository.lagreJournalforingVedtak(vedtak.id, journalpostId, dokumentId)
        jdbcTemplate.update(
            "UPDATE VEDTAK SET VEDTAK_FATTET = ?, DOKUMENT_BESTILLING_ID = ? WHERE ID = ?",
            vedtakFattetDato,
            dokumentBestillingId?.id,
            vedtak.id
        )

        return vedtak.id
    }

    private fun gittFeilendeVedtakDer(
        vedtakFattetDato: LocalDateTime?,
        dokumentBestillingId: DistribusjonBestillingId?,
        aktorId: AktorId = AktorId(randomNumeric(10)),
        veilederIdent: String = randomAlphabetic(1) + randomNumeric(6),
        oppfolgingsenhet: String = randomNumeric(4),
        journalpostId: String = randomNumeric(10),
        dokumentId: String = randomNumeric(9),
        distribusjonsforsok: Int = 12
    ): Long {
        vedtakRepository.opprettUtkast(
            aktorId.get(),
            veilederIdent,
            oppfolgingsenhet
        )
        val vedtak = vedtakRepository.hentUtkast(aktorId.get())
        vedtakRepository.lagreJournalforingVedtak(vedtak.id, journalpostId, dokumentId)
        jdbcTemplate.update(
            "UPDATE VEDTAK SET VEDTAK_FATTET = ?, DOKUMENT_BESTILLING_ID = ? WHERE ID = ?",
            vedtakFattetDato,
            dokumentBestillingId?.id,
            vedtak.id
        )
        retryVedtakdistribusjonRepository.insertJournalpostIdEllerInkrementerAntallRetriesMedEn(journalpostId)
        jdbcTemplate.update(
            "UPDATE RETRY_VEDTAKDISTRIBUSJON SET DISTRIBUSJONSFORSOK = ? WHERE JOURNALPOST_ID = ?",
            distribusjonsforsok,
            journalpostId
        )

        return vedtak.id
    }

    private fun <T> any(): T = Mockito.any()
}

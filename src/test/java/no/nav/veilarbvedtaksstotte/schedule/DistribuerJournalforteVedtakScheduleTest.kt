package no.nav.veilarbvedtaksstotte.schedule

import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DistribuerJournalpostResponsDTO
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient
import no.nav.veilarbvedtaksstotte.domain.DistribusjonBestillingId
import no.nav.veilarbvedtaksstotte.domain.DistribusjonBestillingId.*
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.DistribusjonServiceV2
import no.nav.veilarbvedtaksstotte.service.UnleashService
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import org.apache.commons.lang3.RandomStringUtils.randomAlphabetic
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.*
import java.time.LocalDateTime
import java.time.LocalDateTime.now

class DistribuerJournalforteVedtakScheduleTest : DatabaseTest() {

    lateinit var leaderElection: LeaderElectionClient
    lateinit var unleashService: UnleashService
    lateinit var dokdistribusjonClient: DokdistribusjonClient
    lateinit var distribusjonServiceV2: DistribusjonServiceV2
    lateinit var vedtakRepository: VedtaksstotteRepository
    lateinit var distribuerJournalforteVedtakSchedule: DistribuerJournalforteVedtakSchedule

    @Before
    fun setup() {
        leaderElection = mock(LeaderElectionClient::class.java)
        unleashService = mock(UnleashService::class.java)
        dokdistribusjonClient = mock(DokdistribusjonClient::class.java)
        vedtakRepository = VedtaksstotteRepository(jdbcTemplate, transactor)
        distribusjonServiceV2 = DistribusjonServiceV2(vedtakRepository, dokdistribusjonClient)
        distribuerJournalforteVedtakSchedule = DistribuerJournalforteVedtakSchedule(
            leaderElection = leaderElection,
            distribusjonServiceV2 = distribusjonServiceV2,
            vedtaksstotteRepository = vedtakRepository,
            unleashService = unleashService
        )
    }

    @Test
    fun `henter vedtak som skal distribueres, og distribuerer i batcher med avgrenset størrelse`() {
        `when`(leaderElection.isLeader).thenReturn(true)
        `when`(unleashService.isDokDistScheduleEnabled).thenReturn(true)
        `when`(dokdistribusjonClient.distribuerJournalpost(any()))
            .then { DistribuerJournalpostResponsDTO(randomAlphabetic(10)) }

        val batchStørrelse = 10

        gittFlereVedtakSomIkkeSkalDistribueres()

        val distribueresFørst = (1..batchStørrelse).toList().map {
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

        distribuerJournalforteVedtakSchedule.distribuerJournalforteVedtak()
        distribueresFørst.forEach { assertDistribuert(it) }

        distribuerJournalforteVedtakSchedule.distribuerJournalforteVedtak()
        distribueresAndre.forEach { assertDistribuert(it) }

        verify(dokdistribusjonClient, times(distribueresFørst.size + distribueresAndre.size))
            .distribuerJournalpost(any())

        reset(dokdistribusjonClient)
        distribuerJournalforteVedtakSchedule.distribuerJournalforteVedtak()
        verify(dokdistribusjonClient, never()).distribuerJournalpost(any())
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
        assertNotNull("Vedtak skal være disteribuert", vedtakRepository.hentVedtak(vedtakId).dokumentbestillingId)
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

    private fun <T> any(): T = Mockito.any()
}
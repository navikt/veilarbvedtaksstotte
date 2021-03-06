package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.client.dokument.DokumentSendtDTO
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.*

class KafkaRepubliseringServiceTest : DatabaseTest() {

    lateinit var innsatsbehovService: InnsatsbehovService
    lateinit var vedtaksstotteRepository: VedtaksstotteRepository
    lateinit var kafkaRepubliseringService: KafkaRepubliseringService

    val pageSize = 3

    @Before
    fun setup() {
        vedtaksstotteRepository = VedtaksstotteRepository(jdbcTemplate, transactor)
        innsatsbehovService = mock(InnsatsbehovService::class.java)
        kafkaRepubliseringService = KafkaRepubliseringServiceOverridePageSize(
            vedtaksstotteRepository, innsatsbehovService, pageSize
        )
    }

    @Test
    fun `republiserer innsatsbehov for alle brukere som har vedtak i denne løsningen`() {
        val antallBrukere = pageSize * 4 + 1
        val brukereMedFattetVedtak = lagTilfeldingeAktorIder(antallBrukere)

        DbTestUtils.cleanupDb(jdbcTemplate)
        brukereMedFattetVedtak.map { lagVedtak(it, true) }

        // brukere uten fattet vedtak
        lagTilfeldingeAktorIder(2).map { lagVedtak(it, false) }

        kafkaRepubliseringService.republiserInnsatsbehovVedtaksstotte()

        verify(innsatsbehovService, times(antallBrukere)).republiserKafkaInnsatsbehov(any())
        brukereMedFattetVedtak.forEach {
            verify(innsatsbehovService).republiserKafkaInnsatsbehov(it)
        }
    }

    private fun lagTilfeldingeAktorIder(antall: Int): List<AktorId> {
        return (1..antall)
            .map { AktorId(RandomStringUtils.randomNumeric(5)) }
    }

    private fun <T> any(): T = Mockito.any()

    private fun lagVedtak(aktorId: AktorId, ferdigstill: Boolean) {
        vedtaksstotteRepository.opprettUtkast(aktorId.get(), "veileder", "1234")
        val utkast = vedtaksstotteRepository.hentUtkast(aktorId.get())
        if (ferdigstill) {
            vedtaksstotteRepository.ferdigstillVedtak(
                utkast.id,
                DokumentSendtDTO("journalpostId", "dokumentId")
            )
        }
    }

    class KafkaRepubliseringServiceOverridePageSize(
        override val vedtaksstotteRepository: VedtaksstotteRepository,
        override val innsatsbehovService: InnsatsbehovService,
        override val pageSize: Int
    ) : KafkaRepubliseringService(vedtaksstotteRepository, innsatsbehovService)
}

package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.*
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random.Default.nextInt
import kotlin.random.Random.Default.nextLong

class KafkaRepubliseringServiceTest : DatabaseTest() {

    lateinit var siste14aVedtakService: Siste14aVedtakService
    lateinit var vedtaksstotteRepository: VedtaksstotteRepository
    lateinit var kafkaRepubliseringService: KafkaRepubliseringService
    lateinit var arenaVedtakRepository: ArenaVedtakRepository
    lateinit var kafkaProducerService: KafkaProducerService

    @Before
    fun setup() {
        DbTestUtils.cleanupDb(jdbcTemplate)

        vedtaksstotteRepository = VedtaksstotteRepository(jdbcTemplate, transactor)
        arenaVedtakRepository = ArenaVedtakRepository(jdbcTemplate)
        siste14aVedtakService = mock(Siste14aVedtakService::class.java)
        kafkaProducerService = mock(KafkaProducerService::class.java)
        kafkaRepubliseringService = KafkaRepubliseringService(
            vedtaksstotteRepository, arenaVedtakRepository, siste14aVedtakService, kafkaProducerService
        )
    }

    @Test
    fun `republiserer siste 14a vedtak for alle brukere som har vedtak i denne løsningen eller fra Arena`() {
        val brukereMedFattetVedtakFraDenneLøsningen = lagTilfeldingeAktorIder(tilfeldigAntall())
        brukereMedFattetVedtakFraDenneLøsningen.map { lagreVedtak(it, true) }

        // brukere uten fattet vedtak
        lagTilfeldingeAktorIder(tilfeldigAntall()).map { lagreVedtak(it, false) }

        val brukereMedVedtakFraArena = lagTilfeldingeFnr(tilfeldigAntall())
        brukereMedVedtakFraArena.map { lagreVedtakFraArena(it) }

        kafkaRepubliseringService.republiserSiste14aVedtak()
        verify(
            siste14aVedtakService,
            times(brukereMedFattetVedtakFraDenneLøsningen.size + brukereMedVedtakFraArena.size)
        ).republiserKafkaSiste14aVedtak(any())

        brukereMedFattetVedtakFraDenneLøsningen.forEach {
            verify(siste14aVedtakService).republiserKafkaSiste14aVedtak(it)
        }

        brukereMedVedtakFraArena.forEach {
            verify(siste14aVedtakService).republiserKafkaSiste14aVedtak(it)
        }
    }

    @Test
    fun `republiserer alle fattede vedtak på dvh topic fra eldste til nyeste`() {
        val brukereMedDuplikat = lagTilfeldingeAktorIder(tilfeldigAntall())
            .let { it + it.subList(0, it.size / 2) }
            .shuffled()

        val fattedeVedtak = brukereMedDuplikat.map { lagreVedtak(it, true) }

        kafkaRepubliseringService.republiserVedtak14aFattetDvh(3)

        val captor = ArgumentCaptor.forClass(Vedtak::class.java)
        verify(kafkaProducerService, times(brukereMedDuplikat.size)).sendVedtakFattetDvh(captor.capture())
        assertThat(captor.allValues.map { it.id }).containsExactlyElementsOf(fattedeVedtak.map { it.id }.sorted())
    }

    private fun lagTilfeldingeAktorIder(antall: Int): List<AktorId> {
        return (1..antall)
            .map { AktorId(randomNumeric(10)) }
    }

    private fun lagTilfeldingeFnr(antall: Int): List<Fnr> {
        return (1..antall)
            .map { Fnr(randomNumeric(11)) }
    }

    private fun tilfeldigAntall(): Int {
        return nextInt(10, 30)
    }

    private fun lagreVedtak(aktorId: AktorId, ferdigstill: Boolean): Vedtak {
        vedtaksstotteRepository.opprettUtkast(aktorId.get(), "veileder", "1234")
        val utkast = vedtaksstotteRepository.hentUtkast(aktorId.get())
        if (ferdigstill) {
            vedtaksstotteRepository.ferdigstillVedtak(utkast.id)
        }
        return vedtaksstotteRepository.hentVedtak(utkast.id)
    }

    private fun lagreVedtakFraArena(fnr: Fnr) {
        arenaVedtakRepository.upsertVedtak(ArenaVedtak(
            fnr = fnr,
            innsatsgruppe = ArenaVedtak.ArenaInnsatsgruppe.IKVAL,
            hovedmal = ArenaVedtak.ArenaHovedmal.SKAFFEA,
            fraDato = LocalDate.now(),
            regUser = "REG_USER",
            operationTimestamp = LocalDateTime.now(),
            hendelseId = nextLong(),
            vedtakId = nextLong()
        ))
    }

    private fun <T> any(): T = Mockito.any()
}

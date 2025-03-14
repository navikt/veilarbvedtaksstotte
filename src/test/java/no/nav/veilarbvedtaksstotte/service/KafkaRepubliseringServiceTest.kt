package no.nav.veilarbvedtaksstotte.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random.Default.nextInt
import kotlin.random.Random.Default.nextLong


class KafkaRepubliseringServiceTest : DatabaseTest() {

    val oppfolgingsvedtak14AService = mockk<Oppfolgingsvedtak14aService>()
    val dvhRapporteringService = mockk<DvhRapporteringService>()

    lateinit var vedtaksstotteRepository: VedtaksstotteRepository
    lateinit var kafkaRepubliseringService: KafkaRepubliseringService
    lateinit var arenaVedtakRepository: ArenaVedtakRepository

    @BeforeEach
    fun setup() {
        DbTestUtils.cleanupDb(jdbcTemplate)

        vedtaksstotteRepository = VedtaksstotteRepository(jdbcTemplate, transactor)
        arenaVedtakRepository = ArenaVedtakRepository(jdbcTemplate)
        kafkaRepubliseringService = KafkaRepubliseringService(
            vedtaksstotteRepository, arenaVedtakRepository, oppfolgingsvedtak14AService, dvhRapporteringService
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

        every {
            oppfolgingsvedtak14AService.republiserKafkaSiste14aVedtak(any())
        } answers {}

        kafkaRepubliseringService.republiserSiste14aVedtak()

        verify(exactly = brukereMedFattetVedtakFraDenneLøsningen.size + brukereMedVedtakFraArena.size) {
            oppfolgingsvedtak14AService.republiserKafkaSiste14aVedtak(any())
        }

        brukereMedFattetVedtakFraDenneLøsningen.forEach {
            verify {
                oppfolgingsvedtak14AService.republiserKafkaSiste14aVedtak(it)
            }
        }

        brukereMedVedtakFraArena.forEach {
            verify {
                oppfolgingsvedtak14AService.republiserKafkaSiste14aVedtak(it)
            }
        }
    }

    @Test
    fun `republiserer alle fattede vedtak på dvh topic fra eldste til nyeste`() {
        val brukereMedDuplikat =
            lagTilfeldingeAktorIder(tilfeldigAntall()).let { it + it.subList(0, it.size / 2) }.shuffled()

        val fattedeVedtak = brukereMedDuplikat.map { lagreVedtak(it, true) }

        val captureList = mutableListOf<Vedtak>()

        every {
            dvhRapporteringService.produserVedtakFattetDvhMelding(vedtak = capture(captureList))
        } answers {}

        kafkaRepubliseringService.republiserVedtak14aFattetDvh(3)

        assertThat(captureList.map { it.id }).containsExactlyElementsOf(fattedeVedtak.map { it.id }.sorted())
    }

    private fun lagTilfeldingeAktorIder(antall: Int): List<AktorId> {
        return (1..antall).map { AktorId(randomNumeric(10)) }
    }

    private fun lagTilfeldingeFnr(antall: Int): List<Fnr> {
        return (1..antall).map { Fnr(randomNumeric(11)) }
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
        arenaVedtakRepository.upsertVedtak(
            ArenaVedtak(
                fnr = fnr,
                innsatsgruppe = ArenaVedtak.ArenaInnsatsgruppe.IKVAL,
                hovedmal = ArenaVedtak.ArenaHovedmal.SKAFFEA,
                fraDato = LocalDate.now(),
                regUser = "REG_USER",
                operationTimestamp = LocalDateTime.now(),
                hendelseId = nextLong(),
                vedtakId = nextLong()
            )
        )
    }
}

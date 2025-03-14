package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.oppfolgingsperiode.SisteOppfolgingsperiode
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.SisteOppfolgingPeriodeRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.Oppfolgingsvedtak14aService.Companion.LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE
import no.nav.veilarbvedtaksstotte.utils.TestData
import no.nav.veilarbvedtaksstotte.utils.TestdataGenerator.OppfolgingsvedtakGenerator.genererRandomArenaVedtak
import no.nav.veilarbvedtaksstotte.utils.TestdataGenerator.OppfolgingsvedtakGenerator.genererRandomVedtak
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toLocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

class Oppfolgingsvedtak14aServiceTest {
    private val transactor: TransactionTemplate = mock(TransactionTemplate::class.java)
    private val kafkaProducerService: KafkaProducerService = mock(KafkaProducerService::class.java)
    private val vedtakRepository: VedtaksstotteRepository = mock(VedtaksstotteRepository::class.java)
    private val arenaVedtakRepository: ArenaVedtakRepository = mock(ArenaVedtakRepository::class.java)
    private val aktorOppslagClient: AktorOppslagClient = mock(AktorOppslagClient::class.java)
    private val arenaVedtakService: ArenaVedtakService = mock(ArenaVedtakService::class.java)
    private val sisteOppfolgingPeriodeRepository: SisteOppfolgingPeriodeRepository =
        mock(SisteOppfolgingPeriodeRepository::class.java)

    private val oppfolgingsvedtak14AService: Oppfolgingsvedtak14aService = Oppfolgingsvedtak14aService(
        transactor,
        kafkaProducerService,
        vedtakRepository,
        arenaVedtakRepository,
        aktorOppslagClient,
        arenaVedtakService,
        sisteOppfolgingPeriodeRepository
    )

    @BeforeEach
    fun setup() {
        `when`(aktorOppslagClient.hentIdenter(TestData.TEST_FNR)).thenReturn(
            BrukerIdenter(
                TestData.TEST_FNR,
                AktorId.of(TestData.TEST_AKTOR_ID),
                emptyList(),
                emptyList()
            )
        )
    }

    @Test
    fun `vedtak er gjeldende dersom vedtak er fattet i innevarende oppfolgingsperiode`() {
        `when`(sisteOppfolgingPeriodeRepository.hentInnevarendeOppfolgingsperiode(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(
            SisteOppfolgingsperiode(
                oppfolgingsperiodeId = UUID.randomUUID(),
                aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
                startdato = ZonedDateTime.now().minusDays(11),
                sluttdato = null
            )
        )
        `when`(arenaVedtakRepository.hentVedtakListe(listOf(TestData.TEST_FNR))).thenReturn(emptyList())
        val vedtak = genererRandomVedtak(
            aktorId = TestData.TEST_AKTOR_ID,
            vedtakFattet = LocalDateTime.now().minusDays(10),
        )
        `when`(vedtakRepository.hentSisteVedtak(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(vedtak)

        val gjeldende14aVedtak = oppfolgingsvedtak14AService.hentGjeldende14aVedtak(TestData.TEST_FNR)

        assert(gjeldende14aVedtak != null)
        assertEquals(gjeldende14aVedtak?.aktorId.toString(), TestData.TEST_AKTOR_ID)
    }

    @Test
    fun `vedtak er historisk dersom oppfolgingsperiode er avsluttet`() {
        `when`(sisteOppfolgingPeriodeRepository.hentInnevarendeOppfolgingsperiode(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(
            null
        )
        `when`(arenaVedtakRepository.hentVedtakListe(listOf(TestData.TEST_FNR))).thenReturn(emptyList())
        val vedtak = genererRandomVedtak(
            aktorId = TestData.TEST_AKTOR_ID,
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = Hovedmal.SKAFFE_ARBEID,
            vedtakFattet = LocalDateTime.now().minusDays(1),
        )
        `when`(vedtakRepository.hentSisteVedtak(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(vedtak)

        val gjeldende14aVedtak = oppfolgingsvedtak14AService.hentGjeldende14aVedtak(TestData.TEST_FNR)

        assertNull(gjeldende14aVedtak)
    }

    @Test
    fun `hentGjeldende14aVedtak returnerer null dersom person ikke har vedtak`() {
        `when`(sisteOppfolgingPeriodeRepository.hentInnevarendeOppfolgingsperiode(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(
            SisteOppfolgingsperiode(
                oppfolgingsperiodeId = UUID.randomUUID(),
                aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
                startdato = ZonedDateTime.now().minusDays(1),
                sluttdato = null
            )
        )
        `when`(arenaVedtakRepository.hentVedtakListe(listOf(TestData.TEST_FNR))).thenReturn(emptyList())
        `when`(vedtakRepository.hentSisteVedtak(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(null)

        val gjeldende14aVedtak = oppfolgingsvedtak14AService.hentGjeldende14aVedtak(TestData.TEST_FNR)

        assertNull(gjeldende14aVedtak)
    }

    @Test
    fun `vedtak er gjeldende dersom vedtak er fattet 4 dager før innevarende oppfolgingsperiode`() {
        `when`(sisteOppfolgingPeriodeRepository.hentInnevarendeOppfolgingsperiode(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(
            SisteOppfolgingsperiode(
                oppfolgingsperiodeId = UUID.randomUUID(),
                aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
                startdato = ZonedDateTime.now(),
                sluttdato = null
            )
        )
        `when`(arenaVedtakRepository.hentVedtakListe(listOf(TestData.TEST_FNR))).thenReturn(emptyList())
        val vedtak = genererRandomVedtak(
            aktorId = TestData.TEST_AKTOR_ID,
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = Hovedmal.SKAFFE_ARBEID,
            vedtakFattet = LocalDateTime.now().minusDays(4),
        )
        `when`(vedtakRepository.hentSisteVedtak(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(vedtak)

        val gjeldende14aVedtak = oppfolgingsvedtak14AService.hentGjeldende14aVedtak(TestData.TEST_FNR)

        assert(gjeldende14aVedtak != null)
        assertEquals(gjeldende14aVedtak?.aktorId.toString(), TestData.TEST_AKTOR_ID)
    }

    @Test
    fun `vedtak er historisk dersom vedtak fattet mer enn 4 dager for innevarende oppfolgingsperiode`() {
        `when`(sisteOppfolgingPeriodeRepository.hentInnevarendeOppfolgingsperiode(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(
            SisteOppfolgingsperiode(
                oppfolgingsperiodeId = UUID.randomUUID(),
                aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
                startdato = ZonedDateTime.now(),
                sluttdato = null
            )
        )
        `when`(arenaVedtakRepository.hentVedtakListe(listOf(TestData.TEST_FNR))).thenReturn(emptyList())
        val vedtak = genererRandomVedtak(
            aktorId = TestData.TEST_AKTOR_ID,
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = Hovedmal.SKAFFE_ARBEID,
            vedtakFattet = LocalDateTime.now().minusDays(5),
        )
        `when`(vedtakRepository.hentSisteVedtak(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(vedtak)

        val gjeldende14aVedtak = oppfolgingsvedtak14AService.hentGjeldende14aVedtak(TestData.TEST_FNR)

        assertNull(gjeldende14aVedtak)
    }

    @Test
    fun `vedtak er gjeldende dersom det ble fattet før man startet med oppfølgingsperioder (2017)`() {
        `when`(sisteOppfolgingPeriodeRepository.hentInnevarendeOppfolgingsperiode(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(
            SisteOppfolgingsperiode(
                oppfolgingsperiodeId = UUID.randomUUID(),
                aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
                startdato = LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE,
                sluttdato = null
            )
        )
        val arenaVedtakListe = listOf(
            genererRandomArenaVedtak(
                fnr = TestData.TEST_FNR,
                innsatsgruppe = ArenaVedtak.ArenaInnsatsgruppe.IKVAL,
                hovedmal = ArenaVedtak.ArenaHovedmal.SKAFFEA,
                fraDato = LocalDate.from(LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE.minusDays(3))
            )
        )
        `when`(arenaVedtakRepository.hentVedtakListe(listOf(TestData.TEST_FNR))).thenReturn(arenaVedtakListe)
        `when`(vedtakRepository.hentSisteVedtak(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(null)

        val gjeldende14aVedtak = oppfolgingsvedtak14AService.hentGjeldende14aVedtak(TestData.TEST_FNR)

        assert(gjeldende14aVedtak != null)
        assertEquals(gjeldende14aVedtak?.aktorId.toString(), TestData.TEST_AKTOR_ID)
    }

    @Test
    fun `Ingen gjeldende vedtak dersom vedtak tilhører en annen oppfølgingsperiode`() {
        `when`(sisteOppfolgingPeriodeRepository.hentInnevarendeOppfolgingsperiode(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(
            SisteOppfolgingsperiode(
                oppfolgingsperiodeId = UUID.randomUUID(),
                aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
                startdato = LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE.plusDays(1),
                sluttdato = null
            )
        )
        `when`(arenaVedtakRepository.hentVedtakListe(listOf(TestData.TEST_FNR))).thenReturn(emptyList())
        val vedtak = genererRandomVedtak(
            aktorId = TestData.TEST_AKTOR_ID,
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = Hovedmal.SKAFFE_ARBEID,
            vedtakFattet = toLocalDateTime(LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE.minusDays(3)),
        )
        `when`(vedtakRepository.hentSisteVedtak(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(vedtak)

        val gjeldende14aVedtak = oppfolgingsvedtak14AService.hentGjeldende14aVedtak(TestData.TEST_FNR)

        assertNull(gjeldende14aVedtak)
    }
}

package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.oppfolgingsperiode.SisteOppfolgingsperiode
import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.repository.SisteOppfolgingPeriodeRepository
import no.nav.veilarbvedtaksstotte.service.Gjeldende14aVedtakService.Companion.LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE
import no.nav.veilarbvedtaksstotte.utils.AbstractVedtakIntegrationTest
import no.nav.veilarbvedtaksstotte.utils.TestData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.ZonedDateTime
import java.util.*

class Gjeldende14aVedtakServiceTest  : AbstractVedtakIntegrationTest() {

    private final val siste14aVedtakService: Siste14aVedtakService = Mockito.mock(Siste14aVedtakService::class.java)
    private final val sisteOppfolgingPeriodeRepository: SisteOppfolgingPeriodeRepository = Mockito.mock(SisteOppfolgingPeriodeRepository::class.java)
    final override var aktorOppslagClient: AktorOppslagClient = Mockito.mock(AktorOppslagClient::class.java)

    val gjeldende14aVedtakService = Gjeldende14aVedtakService(siste14aVedtakService, sisteOppfolgingPeriodeRepository, aktorOppslagClient)

    @BeforeEach
    fun setup() {
        `when`(aktorOppslagClient.hentAktorId(TestData.TEST_FNR)).thenReturn(AktorId.of(TestData.TEST_AKTOR_ID))
        `when`(aktorOppslagClient.hentFnr(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(TestData.TEST_FNR)
        `when`(aktorOppslagClient.hentIdenter(TestData.TEST_FNR)).thenReturn(BrukerIdenter(TestData.TEST_FNR, AktorId.of(TestData.TEST_AKTOR_ID), null, null))
    }

    @Test
    fun `vedtak er gjeldende dersom vedtak er fattet i innevarende oppfolgingsperiode`() {
        `when`(sisteOppfolgingPeriodeRepository.hentInnevaerendeOppfolgingsperiode(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(
            SisteOppfolgingsperiode(
                oppfolgingsperiodeId = UUID.randomUUID(),
                aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
                startdato = ZonedDateTime.now().minusDays(1),
                sluttdato = null
            )
        )
        `when`(siste14aVedtakService.siste14aVedtak(TestData.TEST_FNR)).thenReturn(Siste14aVedtak(
            aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
            fattetDato = ZonedDateTime.now(),
            fraArena = false
        ))

        val gjeldende14aVedtak = gjeldende14aVedtakService.hentGjeldende14aVedtak(TestData.TEST_FNR)

        assert(gjeldende14aVedtak != null)
        assertEquals(gjeldende14aVedtak?.aktorId.toString(), TestData.TEST_AKTOR_ID)
    }

    @Test
    fun `vedtak er historisk dersom oppfolgingsperiode er avsluttet`() {
        `when`(sisteOppfolgingPeriodeRepository.hentInnevaerendeOppfolgingsperiode(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(null)
        `when`(siste14aVedtakService.siste14aVedtak(TestData.TEST_FNR)).thenReturn(Siste14aVedtak(
            aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
            fattetDato = ZonedDateTime.now().minusDays(1),
            fraArena = false
        ))

        val gjeldende14aVedtak = gjeldende14aVedtakService.hentGjeldende14aVedtak(TestData.TEST_FNR)

        assertNull(gjeldende14aVedtak)
    }

    @Test
    fun `hentGjeldende14aVedtak returnerer null dersom person ikke har vedtak`() {
        `when`(sisteOppfolgingPeriodeRepository.hentInnevaerendeOppfolgingsperiode(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(
            SisteOppfolgingsperiode(
                oppfolgingsperiodeId = UUID.randomUUID(),
                aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
                startdato = ZonedDateTime.now().minusDays(1),
                sluttdato = null
            )
        )
        `when`(siste14aVedtakService.siste14aVedtak(TestData.TEST_FNR)).thenReturn(null)

        val gjeldende14aVedtak = gjeldende14aVedtakService.hentGjeldende14aVedtak(TestData.TEST_FNR)

        assertNull(gjeldende14aVedtak)
    }

    @Test
    fun `vedtak er gjeldende dersom vedtak er fattet 4 dager før innevarende oppfolgingsperiode`() {
        `when`(sisteOppfolgingPeriodeRepository.hentInnevaerendeOppfolgingsperiode(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(
            SisteOppfolgingsperiode(
                oppfolgingsperiodeId = UUID.randomUUID(),
                aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
                startdato = ZonedDateTime.now(),
                sluttdato = null
            )
        )
        `when`(siste14aVedtakService.siste14aVedtak(TestData.TEST_FNR)).thenReturn(Siste14aVedtak(
            aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
            fattetDato = ZonedDateTime.now().minusDays(4),
            fraArena = false
        ))


        val gjeldende14aVedtak = gjeldende14aVedtakService.hentGjeldende14aVedtak(TestData.TEST_FNR)

        assert(gjeldende14aVedtak != null)
        assertEquals(gjeldende14aVedtak?.aktorId.toString(), TestData.TEST_AKTOR_ID)
    }

    @Test
    fun `vedtak er historisk dersom vedtak fattet mer enn 4 dager for innevarende oppfolgingsperiode`() {
        `when`(sisteOppfolgingPeriodeRepository.hentInnevaerendeOppfolgingsperiode(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(
            SisteOppfolgingsperiode(
                oppfolgingsperiodeId = UUID.randomUUID(),
                aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
                startdato = ZonedDateTime.now(),
                sluttdato = null
            )
        )
        `when`(siste14aVedtakService.siste14aVedtak(TestData.TEST_FNR)).thenReturn(Siste14aVedtak(
            aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
            fattetDato = ZonedDateTime.now().minusDays(5),
            fraArena = false
        ))


        val gjeldende14aVedtak = gjeldende14aVedtakService.hentGjeldende14aVedtak(TestData.TEST_FNR)

        assertNull(gjeldende14aVedtak)
    }

    @Test
    fun `vedtak er gjeldende dersom det ble fattet før man startet med oppfølgingsperioder (2017)`() {
        `when`(sisteOppfolgingPeriodeRepository.hentInnevaerendeOppfolgingsperiode(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(
            SisteOppfolgingsperiode(
                oppfolgingsperiodeId = UUID.randomUUID(),
                aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
                startdato = LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE,
                sluttdato = null
            )
        )
        `when`(siste14aVedtakService.siste14aVedtak(TestData.TEST_FNR)).thenReturn(Siste14aVedtak(
            aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
            fattetDato = LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE.minusDays(3),
            fraArena = false
        ))


        val gjeldende14aVedtak = gjeldende14aVedtakService.hentGjeldende14aVedtak(TestData.TEST_FNR)

        assert(gjeldende14aVedtak != null)
        assertEquals(gjeldende14aVedtak?.aktorId.toString(), TestData.TEST_AKTOR_ID)
    }

    @Test
    fun `Ingen gjeldende vedtak dersom vedtak tilhører en annen oppfølgingsperiode`() {
        `when`(sisteOppfolgingPeriodeRepository.hentInnevaerendeOppfolgingsperiode(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(
            SisteOppfolgingsperiode(
                oppfolgingsperiodeId = UUID.randomUUID(),
                aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
                startdato = LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE.plusDays(1),
                sluttdato = null
            )
        )
        `when`(siste14aVedtakService.siste14aVedtak(TestData.TEST_FNR)).thenReturn(Siste14aVedtak(
            aktorId = AktorId.of(TestData.TEST_AKTOR_ID),
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
            fattetDato = LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE.minusDays(3),
            fraArena = true
        ))


        val gjeldende14aVedtak = gjeldende14aVedtakService.hentGjeldende14aVedtak(TestData.TEST_FNR)

        assertNull(gjeldende14aVedtak)
    }
}

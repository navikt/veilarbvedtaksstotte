package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.service.EnkelReaktiveringService.Reaktivering
import no.nav.veilarbvedtaksstotte.utils.AbstractVedtakIntegrationTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

class EnkelReaktiveringServiceTest : AbstractVedtakIntegrationTest() {

    @Autowired
    lateinit var enkelReaktiveringService: EnkelReaktiveringService

    @MockBean
    lateinit var veilarboppfolgingClient: VeilarboppfolgingClient

    @Test
    fun `setter vedtak som gjeldende dersom reaktivering skjedde samme dag som gjeldende oppfølgingsperiode startet og vedtaket ble fattet tidligere`() {
        val identer = gittBrukerIdenter()
        lagreArenaVedtak(identer.fnr, fraDato = LocalDate.now().minusDays(5))
        val vedtak = lagreFattetVedtak(
            aktorId = identer.aktorId,
            vedtakFattetDato = LocalDateTime.now().minusDays(4),
            gjeldende = false
        )

        val oppfolgingPeriode = OppfolgingPeriodeDTO(ZonedDateTime.now().minusDays(3), null)
        `when`(veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(identer.fnr)).thenReturn(Optional.of(
            oppfolgingPeriode
        ))

        enkelReaktiveringService.behandleEnkeltReaktivertBruker(
            Reaktivering(
                identer.aktorId,
                oppfolgingPeriode.startDato
            )
        )

        assertTrue(vedtakRepository.hentVedtak(vedtak.id).isGjeldende)
    }

    @Test
    fun `setter vedtak som gjeldende dersom reaktivering skjedde samme dag som gjeldende oppfølgingsperiode startet og siste vedtak ble fattet`() {
        val identer = gittBrukerIdenter()
        lagreArenaVedtak(identer.fnr, fraDato = LocalDate.now().minusDays(5))
        val vedtak = lagreFattetVedtak(
            aktorId = identer.aktorId,
            vedtakFattetDato = LocalDateTime.now().minusDays(2),
            gjeldende = false
        )

        val oppfolgingPeriode = OppfolgingPeriodeDTO(ZonedDateTime.now().minusDays(2), null)
        `when`(veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(identer.fnr)).thenReturn(Optional.of(
            oppfolgingPeriode
        ))

        enkelReaktiveringService.behandleEnkeltReaktivertBruker(
            Reaktivering(
                identer.aktorId,
                oppfolgingPeriode.startDato
            )
        )

        assertTrue(vedtakRepository.hentVedtak(vedtak.id).isGjeldende)
    }

    @Test
    fun `setter ikke vedtak som gjeldende det finnes et nyere fra Arena`() {
        val identer = gittBrukerIdenter()
        lagreArenaVedtak(identer.fnr, fraDato = LocalDate.now().minusDays(4))
        val vedtak = lagreFattetVedtak(
            aktorId = identer.aktorId,
            vedtakFattetDato = LocalDateTime.now().minusDays(5),
            gjeldende = false
        )

        val oppfolgingPeriode = OppfolgingPeriodeDTO(ZonedDateTime.now().minusDays(3), null)
        `when`(veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(identer.fnr)).thenReturn(Optional.of(
            oppfolgingPeriode
        ))

        enkelReaktiveringService.behandleEnkeltReaktivertBruker(
            Reaktivering(
                identer.aktorId,
                oppfolgingPeriode.startDato
            )
        )

        assertFalse(vedtakRepository.hentVedtak(vedtak.id).isGjeldende)
    }

    @Test
    fun `gjør ingenting dersom bruker ikke har en gjeldende oppfølgingsperiode`() {
        val identer = gittBrukerIdenter()
        val vedtak = lagreFattetVedtak(
            aktorId = identer.aktorId,
            vedtakFattetDato = LocalDateTime.now().minusDays(4),
            gjeldende = false
        )

        `when`(veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(identer.fnr)).thenReturn(Optional.empty())

        enkelReaktiveringService.behandleEnkeltReaktivertBruker(
            Reaktivering(
                identer.aktorId,
                ZonedDateTime.now()
            )
        )

        assertFalse(vedtakRepository.hentVedtak(vedtak.id).isGjeldende)
    }

    @Test
    fun `gjør ingenting dersom dag for reaktivering er ulik dag på gjeldende oppfølgingsperiode`() {
        val identer = gittBrukerIdenter()
        val vedtak = lagreFattetVedtak(
            aktorId = identer.aktorId,
            vedtakFattetDato = LocalDateTime.now().minusDays(4),
            gjeldende = false
        )

        val oppfolgingPeriode = OppfolgingPeriodeDTO(ZonedDateTime.now().minusDays(3), null)
        `when`(veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(identer.fnr)).thenReturn(Optional.of(
            oppfolgingPeriode
        ))

        enkelReaktiveringService.behandleEnkeltReaktivertBruker(
            Reaktivering(
                identer.aktorId,
                oppfolgingPeriode.startDato.plusDays(1)
            )
        )

        assertFalse(vedtakRepository.hentVedtak(vedtak.id).isGjeldende)
    }

    @Test
    fun `gjør ingenting dersom reaktivering skjedde før siste vedtak ble fattet`() {
        val identer = gittBrukerIdenter()
        val vedtak = lagreFattetVedtak(
            aktorId = identer.aktorId,
            vedtakFattetDato = LocalDateTime.now().minusDays(2),
            gjeldende = false
        )

        val oppfolgingPeriode = OppfolgingPeriodeDTO(ZonedDateTime.now().minusDays(3), null)
        `when`(veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(identer.fnr)).thenReturn(Optional.of(
            oppfolgingPeriode
        ))

        enkelReaktiveringService.behandleEnkeltReaktivertBruker(
            Reaktivering(
                identer.aktorId,
                oppfolgingPeriode.startDato
            )
        )

        assertFalse(vedtakRepository.hentVedtak(vedtak.id).isGjeldende)
    }
}

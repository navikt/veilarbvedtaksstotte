package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe.Companion.tilInnsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak.HovedmalMedOkeDeltakelse.Companion.fraArenaHovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak.HovedmalMedOkeDeltakelse.Companion.fraHovedmal
import no.nav.veilarbvedtaksstotte.service.EnkelReaktiveringService.Reaktivering
import no.nav.veilarbvedtaksstotte.utils.AbstractVedtakIntegrationTest
import no.nav.veilarbvedtaksstotte.utils.TimeUtils
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
    lateinit var kafkaProducerService: KafkaProducerService

    @MockBean
    lateinit var veilarboppfolgingClient: VeilarboppfolgingClient

    @Test
    fun `republiserer siste vedtak når det er fra Arena`() {

        val identer = gittBrukerIdenter()
        lagreFattetVedtak(
            aktorId = identer.aktorId,
            vedtakFattetDato = LocalDateTime.now().minusDays(5),
            gjeldende = false
        )
        val arenaVedtak = lagreArenaVedtak(identer.fnr, fraDato = LocalDate.now().minusDays(4))

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

        val forventetSiste14aVedtak = Siste14aVedtak(
            identer.aktorId,
            tilInnsatsgruppe(arenaVedtak.innsatsgruppe),
            fraArenaHovedmal(arenaVedtak.hovedmal),
            fattetDato = TimeUtils.toZonedDateTime(arenaVedtak.fraDato.atStartOfDay()),
            fraArena = true
        )

        verify(kafkaProducerService).sendSiste14aVedtak(forventetSiste14aVedtak)
    }

    @Test
    fun `setter vedtak som gjeldende og republiserer siste vedtak når det er fra Vedtaksstøtte`() {
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

        assertFalse(vedtak.isGjeldende)

        val forventetSiste14aVedtak = Siste14aVedtak(
            identer.aktorId,
            vedtak.innsatsgruppe,
            fraHovedmal(vedtak.hovedmal),
            fattetDato = TimeUtils.toZonedDateTime(vedtak.vedtakFattet),
            fraArena = false
        )

        assertTrue(vedtakRepository.hentVedtak(vedtak.id).isGjeldende)
        verify(kafkaProducerService).sendSiste14aVedtak(forventetSiste14aVedtak)
    }

    @Test
    fun `gjør ingenting dersom ingen vedtak`() {
        val identer = gittBrukerIdenter()

        enkelReaktiveringService.behandleEnkeltReaktivertBruker(
            Reaktivering(
                identer.aktorId,
                ZonedDateTime.now()
            )
        )

        verify(kafkaProducerService, never()).sendSiste14aVedtak(any())
    }

    @Test
    fun `gjør ingenting dersom bruker ikke har en gjeldende oppfølgingsperiode`() {
        val identer = gittBrukerIdenter()
        lagreArenaVedtak(identer.fnr, fraDato = LocalDate.now().minusDays(4))

        `when`(veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(identer.fnr)).thenReturn(Optional.empty())

        enkelReaktiveringService.behandleEnkeltReaktivertBruker(
            Reaktivering(
                identer.aktorId,
                ZonedDateTime.now()
            )
        )

        verify(kafkaProducerService, never()).sendSiste14aVedtak(any())
    }

    @Test
    fun `gjør ingenting dersom dag for reaktivering er ulik dag på gjeldende oppfølgingsperiode`() {
        val identer = gittBrukerIdenter()
        lagreArenaVedtak(identer.fnr, fraDato = LocalDate.now().minusDays(4))

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

        verify(kafkaProducerService, never()).sendSiste14aVedtak(any())
    }

    @Test
    fun `gjør ingenting dersom bruker har et gjeldende vedtak`() {
        val identer = gittBrukerIdenter()
        val vedtak = lagreFattetVedtak(
            aktorId = identer.aktorId,
            vedtakFattetDato = LocalDateTime.now().minusDays(4),
            gjeldende = true
        )

        val oppfolgingPeriode = OppfolgingPeriodeDTO(ZonedDateTime.now().minusDays(3), null)
        `when`(veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(identer.fnr)).thenReturn(Optional.of(
            oppfolgingPeriode
        ))

        assertTrue(vedtak.isGjeldende)

        enkelReaktiveringService.behandleEnkeltReaktivertBruker(
            Reaktivering(
                identer.aktorId,
                oppfolgingPeriode.startDato
            )
        )

        verify(kafkaProducerService, never()).sendSiste14aVedtak(any())
    }

    @Test
    fun `gjør ingenting dersom reaktivering skjedde før siste vedtak ble fattet`() {
        val identer = gittBrukerIdenter()
        lagreArenaVedtak(identer.fnr, fraDato = LocalDate.now().minusDays(2))

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

        verify(kafkaProducerService, never()).sendSiste14aVedtak(any())
    }

    @Test
    fun `behandler reaktivering dersom reaktivering skjedde samme dag som gjeldende oppfølgingsperiode startet og siste vedtak ble fattet`() {
        val identer = gittBrukerIdenter()
        lagreArenaVedtak(identer.fnr, fraDato = LocalDate.now().minusDays(2))

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

        verify(kafkaProducerService).sendSiste14aVedtak(any())
    }
}

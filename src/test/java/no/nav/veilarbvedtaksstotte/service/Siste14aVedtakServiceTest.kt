package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaHovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.utils.AbstractVedtakIntegrationTest
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.now
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.LocalDate

class Siste14aVedtakServiceTest : AbstractVedtakIntegrationTest() {

    @Autowired
    lateinit var siste14aVedtakService: Siste14aVedtakService

    @MockBean
    lateinit var kafkaProducerService: KafkaProducerService

    @BeforeEach
    fun before() {
        reset(kafkaProducerService)
    }

    @Test
    fun `siste 14a vedtak er null dersom, ny løsning har null, Arena har null`() {
        val identer = gittBrukerIdenter()
        assertAntallVedtakFraArena(identer, 0)
        assertFattedeVedtakFraNyLøsning(identer, 0)
        assertSiste14aVedtak(identer, null)
    }

    @Test
    fun `siste 14a vedtak fra ny løsning dersom, ny løsning har vedtak, Arena har null`() {

        val identer = gittBrukerIdenter()

        val fattetDato = now()

        lagreFattetVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.SKAFFE_ARBEID,
            vedtakFattetDato = fattetDato
        )

        assertAntallVedtakFraArena(identer, 0)
        assertSiste14aVedtak(
            identer,
            Siste14aVedtak(
                aktorId = identer.aktorId,
                innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
                fattetDato = toZonedDateTime(fattetDato),
                fraArena = false
            )
        )
    }

    @Test
    fun `siste 14a vedtak fra ny løsning dersom nyere vedtak fra ny løsning enn fra Arena`() {
        val identer = gittBrukerIdenter()
        val fattetDato = now().minusDays(3)

        lagreArenaVedtak(
            fnr = identer.fnr,
            fraDato = LocalDate.now().minusDays(4),
            innsatsgruppe = ArenaInnsatsgruppe.VARIG,
            hovedmal = ArenaHovedmal.SKAFFEA
        )

        lagreFattetVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            vedtakFattetDato = fattetDato
        )

        assertAntallVedtakFraArena(identer, 1)
        assertSiste14aVedtak(
            identer = identer,
            forventet = Siste14aVedtak(
                identer.aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID,
                fattetDato = toZonedDateTime(fattetDato),
                fraArena = false
            )
        )
    }

    @Test
    fun `siste 14a vedtak fra ny løsning dersom, ny løsning har vedtak, Arena har eldre fra samme dag`() {
        val identer = gittBrukerIdenter()
        val fattetDato = now().minusDays(3).plusMinutes(1)

        lagreArenaVedtak(
            fnr = identer.fnr,
            fraDato = LocalDate.now().minusDays(3),
            innsatsgruppe = ArenaInnsatsgruppe.VARIG,
            hovedmal = ArenaHovedmal.SKAFFEA,
            operationTimestamp = now().minusDays(3)
        )

        lagreFattetVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            vedtakFattetDato = fattetDato
        )

        assertAntallVedtakFraArena(identer, 1)
        assertSiste14aVedtak(
            identer,
            Siste14aVedtak(
                aktorId = identer.aktorId,
                innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                hovedmal = HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID,
                fattetDato = toZonedDateTime(fattetDato),
                fraArena = false
            )
        )
    }

    @Test
    fun `siste 14a vedtak er fra Arena dersom, ny løsning har null, Arena vedtak`() {
        val identer = gittBrukerIdenter()
        val fattetDato = LocalDate.now().minusDays(1)

        lagreArenaVedtak(
            fnr = identer.fnr,
            fraDato = fattetDato,
            innsatsgruppe = ArenaInnsatsgruppe.BATT,
            hovedmal = ArenaHovedmal.OKEDELT
        )

        assertAntallVedtakFraArena(identer, 1)
        assertFattedeVedtakFraNyLøsning(identer, 0)
        assertSiste14aVedtak(
            identer,
            Siste14aVedtak(
                aktorId = identer.aktorId,
                innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                hovedmal = HovedmalMedOkeDeltakelse.OKE_DELTAKELSE,
                fattetDato = toZonedDateTime(fattetDato.atStartOfDay()),
                fraArena = true
            )
        )
    }

    @Test
    fun `siste 14a vedtak er fra Arena dersom nyere vedtak fra Arena enn fra ny løsning`() {
        val identer = gittBrukerIdenter()
        val fattetDato = LocalDate.now().minusDays(4)

        lagreArenaVedtak(
            fnr = identer.fnr,
            fraDato = fattetDato,
            innsatsgruppe = ArenaInnsatsgruppe.VARIG,
            hovedmal = ArenaHovedmal.SKAFFEA
        )

        lagreFattetVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            vedtakFattetDato = now().minusDays(5)
        )

        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertAntallVedtakFraArena(identer, 1)
        assertSiste14aVedtak(
            identer,
            Siste14aVedtak(
                identer.aktorId, Innsatsgruppe.VARIG_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
                fattetDato = toZonedDateTime(fattetDato.atStartOfDay()),
                fraArena = true
            )
        )
    }

    @Test
    fun `siste 14a vedtak er fra Arena dersom, ny løsning har vedtak, Arena har nyere fra samme dag`() {
        val identer = gittBrukerIdenter()
        val fattetDato = LocalDate.now().minusDays(4)

        lagreArenaVedtak(
            fnr = identer.fnr,
            fraDato = fattetDato,
            innsatsgruppe = ArenaInnsatsgruppe.VARIG,
            hovedmal = ArenaHovedmal.SKAFFEA,
            operationTimestamp = now().minusDays(4).plusMinutes(1)
        )

        lagreFattetVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            vedtakFattetDato = now().minusDays(4)
        )

        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertAntallVedtakFraArena(identer, 1)
        assertSiste14aVedtak(
            identer,
            Siste14aVedtak(
                identer.aktorId, Innsatsgruppe.VARIG_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
                fattetDato = toZonedDateTime(fattetDato.atStartOfDay()),
                fraArena = true
            )
        )
    }

    @Test
    fun `siste 14a vedtak er siste fra Arena dersom, ny løsning har null, Arena har også på historiske fnr`() {
        val identer = gittBrukerIdenter(antallHistoriskeFnr = 3)
        val fattetDato = LocalDate.now().minusDays(4)

        lagreArenaVedtak(
            fnr = identer.fnr,
            fraDato = fattetDato,
            innsatsgruppe = ArenaInnsatsgruppe.VARIG,
            hovedmal = ArenaHovedmal.SKAFFEA
        )

        identer.historiskeFnr.forEachIndexed { index, fnr ->
            lagreArenaVedtak(
                fnr = fnr,
                fraDato = LocalDate.now().minusDays(5 + index.toLong()),
                innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                hovedmal = ArenaHovedmal.OKEDELT
            )
        }

        assertFattedeVedtakFraNyLøsning(identer, 0)
        assertAntallVedtakFraArena(identer, 4)
        assertSiste14aVedtak(
            identer,
            Siste14aVedtak(
                identer.aktorId, Innsatsgruppe.VARIG_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
                fattetDato = toZonedDateTime(fattetDato.atStartOfDay()),
                fraArena = true
            )
        )
    }

    @Test
    fun `siste 14a vedtak er siste fra Arena dersom, ny løsning har null, Arena har bare på historiske fnr`() {
        val identer = gittBrukerIdenter(antallHistoriskeFnr = 4)
        val fattetDato = LocalDate.now().minusDays(4)

        lagreArenaVedtak(
            fnr = identer.historiskeFnr[1],
            fraDato = fattetDato,
            innsatsgruppe = ArenaInnsatsgruppe.IKVAL,
            hovedmal = ArenaHovedmal.BEHOLDEA
        )

        identer.historiskeFnr.forEachIndexed { index, fnr ->
            if (index != 1) {
                lagreArenaVedtak(
                    fnr = fnr,
                    fraDato = LocalDate.now().minusDays(5 + index.toLong()),
                    innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                    hovedmal = ArenaHovedmal.OKEDELT
                )
            }
        }

        assertFattedeVedtakFraNyLøsning(identer, 0)
        assertAntallVedtakFraArena(identer, 4)
        assertSiste14aVedtak(
            identer,
            Siste14aVedtak(
                identer.aktorId, Innsatsgruppe.STANDARD_INNSATS, HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID,
                fattetDato = toZonedDateTime(fattetDato.atStartOfDay()),
                fraArena = true
            )
        )
    }

    @Test
    fun `siste 14a vedtak oppdateres ved melding om nytt vedtak fra Arena`() {
        val identer = gittBrukerIdenter()
        val fattetDato = LocalDate.now()

        assertAntallVedtakFraArena(identer, 0)

        siste14aVedtakService.behandleEndringFraArena(
            arenaVedtak(
                fnr = identer.fnr,
                innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                hovedmal = ArenaHovedmal.OKEDELT,
                fraDato = fattetDato
            )
        )

        val forventetSiste14aVedtak = Siste14aVedtak(
            identer.aktorId,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
            HovedmalMedOkeDeltakelse.OKE_DELTAKELSE,
            fattetDato = toZonedDateTime(fattetDato.atStartOfDay()),
            fraArena = true
        )

        verify(kafkaProducerService).sendSiste14aVedtak(eq(forventetSiste14aVedtak))

        assertSiste14aVedtak(identer, forventetSiste14aVedtak)
    }

    @Test
    fun `siste 14a vedtak oppdateres ved melding om nytt vedtak fra Arena som er nyere enn gjeldende vedtak fra ny løsning som settes til historisk`() {
        val identer = gittBrukerIdenter()
        val fattetDato = LocalDate.now().minusDays(2)

        lagreFattetVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            vedtakFattetDato = now().minusDays(3)
        )

        assertAntallVedtakFraArena(identer, 0)
        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertNotNull(vedtakRepository.hentGjeldendeVedtak(identer.aktorId.get()))

        siste14aVedtakService.behandleEndringFraArena(
            arenaVedtak(
                fnr = identer.fnr,
                innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                hovedmal = ArenaHovedmal.OKEDELT,
                fraDato = fattetDato
            )
        )

        val forventetSiste14aVedtak = Siste14aVedtak(
            identer.aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, HovedmalMedOkeDeltakelse.OKE_DELTAKELSE,
            fattetDato = toZonedDateTime(fattetDato.atStartOfDay()),
            fraArena = true
        )

        verify(kafkaProducerService).sendSiste14aVedtak(eq(forventetSiste14aVedtak))

        assertSiste14aVedtak(identer, forventetSiste14aVedtak)
        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertNull(vedtakRepository.hentGjeldendeVedtak(identer.aktorId.get()))
    }

    @Test
    fun `siste 14a vedtak oppdateres ikke dersom melding stammer fra ny løsning`() {
        val identer = gittBrukerIdenter()
        val fattetDato = now()

        lagreFattetVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            vedtakFattetDato = fattetDato
        )

        assertAntallVedtakFraArena(identer, 0)
        assertFattedeVedtakFraNyLøsning(identer, 1)

        siste14aVedtakService.behandleEndringFraArena(
            arenaVedtak(
                fnr = identer.fnr,
                innsatsgruppe = ArenaInnsatsgruppe.BATT,
                hovedmal = ArenaHovedmal.BEHOLDEA,
                regUser = "MODIA"
            )
        )

        val forventetSiste14aVedtak = Siste14aVedtak(
            identer.aktorId,
            Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID,
            fattetDato = toZonedDateTime(fattetDato),
            fraArena = false
        )

        verify(kafkaProducerService, never()).sendSiste14aVedtak(any())

        assertSiste14aVedtak(identer, forventetSiste14aVedtak)
        assertFattedeVedtakFraNyLøsning(identer, 1)
    }

    @Test
    fun `siste 14a vedtak oppdateres ikke dersom bruker har nyere vedtak fra ny løsning som beholdes som gjeldende`() {
        val identer = gittBrukerIdenter()
        val fattetDato = now().minusDays(2)

        lagreFattetVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = Hovedmal.SKAFFE_ARBEID,
            vedtakFattetDato = fattetDato
        )

        assertAntallVedtakFraArena(identer, 0)
        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertNotNull(vedtakRepository.hentGjeldendeVedtak(identer.aktorId.get()))

        siste14aVedtakService.behandleEndringFraArena(
            arenaVedtak(
                fnr = identer.fnr,
                innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                hovedmal = ArenaHovedmal.BEHOLDEA,
                fraDato = LocalDate.now().minusDays(3)
            )
        )

        val forventetSiste14aVedtak = Siste14aVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
            fattetDato = toZonedDateTime(fattetDato),
            fraArena = false
        )

        verify(kafkaProducerService, never()).sendSiste14aVedtak(any())

        assertSiste14aVedtak(identer, forventetSiste14aVedtak)
        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertNotNull(vedtakRepository.hentGjeldendeVedtak(identer.aktorId.get()))
    }

    @Test
    fun `siste 14a vedtak oppdateres ikke dersom bruker har nyere vedtak fra Arena fra før`() {
        val identer = gittBrukerIdenter()
        val fattetDato = LocalDate.now()
        lagreArenaVedtak(
            fnr = identer.fnr,
            fraDato = fattetDato,
            innsatsgruppe = ArenaInnsatsgruppe.BFORM,
            hovedmal = ArenaHovedmal.OKEDELT,
            hendelseId = 6
        )

        assertAntallVedtakFraArena(identer, 1)

        siste14aVedtakService.behandleEndringFraArena(
            arenaVedtak(
                fnr = identer.fnr,
                fraDato = LocalDate.now().minusDays(1),
                innsatsgruppe = ArenaInnsatsgruppe.IKVAL,
                hovedmal = ArenaHovedmal.SKAFFEA,
                hendelseId = 5
            )
        )

        verify(kafkaProducerService, never()).sendSiste14aVedtak(any())

        assertSiste14aVedtak(
            identer, Siste14aVedtak(
                aktorId = identer.aktorId,
                innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                hovedmal = HovedmalMedOkeDeltakelse.OKE_DELTAKELSE,
                fattetDato = toZonedDateTime(fattetDato.atStartOfDay()),
                fraArena = true
            )
        )
    }

    @Test
    fun `siste 14a vedtak oppdateres ikke dersom bruker har nyere vedtak fra Arena fra før med annet fnr`() {
        val identer = gittBrukerIdenter(antallHistoriskeFnr = 1)
        val fattetDato = LocalDate.now()

        lagreArenaVedtak(
            fnr = identer.fnr,
            fraDato = fattetDato,
            innsatsgruppe = ArenaInnsatsgruppe.BFORM,
            hovedmal = ArenaHovedmal.OKEDELT
        )

        assertAntallVedtakFraArena(identer, 1)

        siste14aVedtakService.behandleEndringFraArena(
            arenaVedtak(
                fnr = identer.historiskeFnr[0],
                fraDato = LocalDate.now().minusDays(1),
                innsatsgruppe = ArenaInnsatsgruppe.IKVAL,
                hovedmal = ArenaHovedmal.SKAFFEA
            )
        )

        verify(kafkaProducerService, never()).sendSiste14aVedtak(any())

        assertSiste14aVedtak(
            identer, Siste14aVedtak(
                identer.aktorId,
                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                HovedmalMedOkeDeltakelse.OKE_DELTAKELSE,
                fattetDato = toZonedDateTime(fattetDato.atStartOfDay()),
                fraArena = true
            )
        )
    }

    fun assertFattedeVedtakFraNyLøsning(identer: BrukerIdenter, antall: Int) {
        assertEquals(
            antall,
            vedtakRepository.hentFattedeVedtak(identer.aktorId.get()).size,
            "Antall vedtak fra ny løsning"
        )
    }

    fun assertAntallVedtakFraArena(identer: BrukerIdenter, antall: Int) {
        assertEquals(
            antall,
            arenaVedtakRepository.hentVedtakListe(identer.historiskeFnr.plus(identer.fnr)).size,
            "Antall vedtak fra Arena"
        )
    }

    fun assertSiste14aVedtak(identer: BrukerIdenter, forventet: Siste14aVedtak?) {
        assertEquals(
            forventet,
            siste14aVedtakService.siste14aVedtak(identer.fnr),
            "Siste 14a vedtak"
        )
    }

}

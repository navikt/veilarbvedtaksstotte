package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.veilarbvedtaksstotte.domain.vedtak.*
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaHovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.utils.AbstractVedtakIntegrationTest
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.now
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toZonedDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.LocalDate
import java.util.*

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
        val referanse = UUID.randomUUID()

        val lagretVedtak = lagreFattetVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.SKAFFE_ARBEID,
            vedtakFattetDato = fattetDato,
            referanse = referanse
        )

        assertAntallVedtakFraArena(identer, 0)
        assertSiste14aVedtak(
            identer,
            Siste14aVedtak(
                aktorId = identer.aktorId,
                innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
                fattetDato = toZonedDateTime(fattetDato),
                fraArena = false,
                vedtakId = Siste14aVedtak.VedtakIdVedtaksstotte(
                    id = lagretVedtak.id,
                    referanse = referanse
                )
            )
        )
    }

    @Test
    fun `siste 14a vedtak fra ny løsning dersom nyere vedtak fra ny løsning enn fra Arena`() {
        val identer = gittBrukerIdenter()
        val fattetDato = now().minusDays(3)
        val referanse = UUID.randomUUID()

        lagreArenaVedtak(
            fnr = identer.fnr,
            fraDato = LocalDate.now().minusDays(4),
            innsatsgruppe = ArenaInnsatsgruppe.VARIG,
            hovedmal = ArenaHovedmal.SKAFFEA
        )

        val lagretVedtak = lagreFattetVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            vedtakFattetDato = fattetDato,
            referanse = referanse
        )

        assertAntallVedtakFraArena(identer, 1)
        assertSiste14aVedtak(
            identer = identer,
            forventet = Siste14aVedtak(
                identer.aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID,
                fattetDato = toZonedDateTime(fattetDato),
                fraArena = false,
                vedtakId = Siste14aVedtak.VedtakIdVedtaksstotte(
                    id = lagretVedtak.id,
                    referanse = referanse
                )
            )
        )
    }

    @Test
    fun `siste 14a vedtak fra ny løsning dersom, ny løsning har vedtak, Arena har eldre fra samme dag`() {
        val identer = gittBrukerIdenter()
        val fattetDato = now().minusDays(3).plusMinutes(1)
        val referanse = UUID.randomUUID()

        lagreArenaVedtak(
            fnr = identer.fnr,
            fraDato = LocalDate.now().minusDays(3),
            innsatsgruppe = ArenaInnsatsgruppe.VARIG,
            hovedmal = ArenaHovedmal.SKAFFEA,
            operationTimestamp = now().minusDays(3)
        )

        val lagretVedtak = lagreFattetVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            vedtakFattetDato = fattetDato,
            referanse = referanse
        )

        assertAntallVedtakFraArena(identer, 1)
        assertSiste14aVedtak(
            identer,
            Siste14aVedtak(
                aktorId = identer.aktorId,
                innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                hovedmal = HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID,
                fattetDato = toZonedDateTime(fattetDato),
                fraArena = false,
                vedtakId = Siste14aVedtak.VedtakIdVedtaksstotte(
                    id = lagretVedtak.id,
                    referanse = referanse
                )
            )
        )
    }

    @Test
    fun `siste 14a vedtak er fra Arena dersom, ny løsning har null, Arena vedtak`() {
        val identer = gittBrukerIdenter()
        val fattetDato = LocalDate.now().minusDays(1)

        val lagretArenaVedtak = lagreArenaVedtak(
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
                fraArena = true,
                vedtakId = Siste14aVedtak.VedtakIdArena(
                    id = 1
                )
            )
        )
    }

    @Test
    fun `siste 14a vedtak er fra Arena dersom nyere vedtak fra Arena enn fra ny løsning`() {
        val identer = gittBrukerIdenter()
        val fattetDato = LocalDate.now().minusDays(4)

        val lagretArenaVedtak = lagreArenaVedtak(
            fnr = identer.fnr,
            fraDato = fattetDato,
            innsatsgruppe = ArenaInnsatsgruppe.VARIG,
            hovedmal = ArenaHovedmal.SKAFFEA
        )

        val lagretVedtak = lagreFattetVedtak(
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
                fraArena = true,
                vedtakId = Siste14aVedtak.VedtakIdArena(
                    id = 1
                )
            )
        )
    }

    @Test
    fun `siste 14a vedtak er fra Arena dersom, ny løsning har vedtak, Arena har nyere fra samme dag`() {
        val identer = gittBrukerIdenter()
        val fattetDato = LocalDate.now().minusDays(4)

        val lagretArenaVedtak = lagreArenaVedtak(
            fnr = identer.fnr,
            fraDato = fattetDato,
            innsatsgruppe = ArenaInnsatsgruppe.VARIG,
            hovedmal = ArenaHovedmal.SKAFFEA,
            operationTimestamp = now().minusDays(4).plusMinutes(1)
        )

        val lagretVedtak = lagreFattetVedtak(
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
                fraArena = true,
                vedtakId = Siste14aVedtak.VedtakIdArena(
                    id = lagretArenaVedtak.vedtakId
                )
            )
        )
    }

    @Test
    fun `siste 14a vedtak er siste fra Arena dersom, ny løsning har null, Arena har også på historiske fnr`() {
        val identer = gittBrukerIdenter(antallHistoriskeFnr = 3)
        val fattetDato = LocalDate.now().minusDays(4)

        val lagretArenaVedtakPaAktivFnr = lagreArenaVedtak(
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
                fraArena = true,
                vedtakId = Siste14aVedtak.VedtakIdArena(
                    lagretArenaVedtakPaAktivFnr.vedtakId
                )
            )
        )
    }

    @Test
    fun `siste 14a vedtak er siste fra Arena dersom, ny løsning har null, Arena har bare på historiske fnr`() {
        val identer = gittBrukerIdenter(antallHistoriskeFnr = 4)
        val fattetDato = LocalDate.now().minusDays(4)

        val lagretArenaVedtakPaHistoriskFnr = lagreArenaVedtak(
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
                fraArena = true,
                vedtakId = Siste14aVedtak.VedtakIdArena(
                    lagretArenaVedtakPaHistoriskFnr.vedtakId
                )
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

        val forventetSiste14aVedtakKafkaDTO = Siste14aVedtakKafkaDTO(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
            hovedmal = HovedmalMedOkeDeltakelse.OKE_DELTAKELSE,
            fattetDato = toZonedDateTime(fattetDato.atStartOfDay()),
            fraArena = true
        )
        val forventetSiste14aVedtak =
            Siste14aVedtak(
                aktorId = identer.aktorId,
                innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                hovedmal = HovedmalMedOkeDeltakelse.OKE_DELTAKELSE,
                fattetDato = toZonedDateTime(fattetDato.atStartOfDay()),
                fraArena = true,
                vedtakId = Siste14aVedtak.VedtakIdArena(
                    id = 1
                )
            )


        verify(kafkaProducerService).sendSiste14aVedtak(eq(forventetSiste14aVedtakKafkaDTO))

        assertSiste14aVedtak(identer, forventetSiste14aVedtak)
    }

    @Test
    fun `siste 14a vedtak oppdateres ved melding om nytt vedtak fra Arena som er nyere enn gjeldende vedtak fra ny løsning som settes til historisk`() {
        val identer = gittBrukerIdenter()
        val fattetDato = LocalDate.now().minusDays(2)

        val lagretVedtak = lagreFattetVedtak(
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

        val forventetSiste14aVedtakKafkaDTO = Siste14aVedtakKafkaDTO(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
            hovedmal = HovedmalMedOkeDeltakelse.OKE_DELTAKELSE,
            fattetDato = toZonedDateTime(fattetDato.atStartOfDay()),
            fraArena = true
        )
        val forventetSiste14aVedtak = Siste14aVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
            hovedmal = HovedmalMedOkeDeltakelse.OKE_DELTAKELSE,
            fattetDato = toZonedDateTime(fattetDato.atStartOfDay()),
            fraArena = true,
            vedtakId = Siste14aVedtak.VedtakIdArena(
                id = 1
            )
        )


        verify(kafkaProducerService).sendSiste14aVedtak(eq(forventetSiste14aVedtakKafkaDTO))

        assertSiste14aVedtak(identer, forventetSiste14aVedtak)
        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertNull(vedtakRepository.hentGjeldendeVedtak(identer.aktorId.get()))
    }

    @Test
    fun `siste 14a vedtak oppdateres ikke dersom melding stammer fra ny løsning`() {
        val identer = gittBrukerIdenter()
        val fattetDato = now()
        val referanse = UUID.randomUUID()

        val lagretVedtak = lagreFattetVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            vedtakFattetDato = fattetDato,
            referanse = referanse
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
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID,
            fattetDato = toZonedDateTime(fattetDato),
            fraArena = false,
            vedtakId = Siste14aVedtak.VedtakIdVedtaksstotte(
                id = lagretVedtak.id,
                referanse = referanse
            )
        )

        verify(kafkaProducerService, never()).sendSiste14aVedtak(any())

        assertSiste14aVedtak(identer, forventetSiste14aVedtak)
        assertFattedeVedtakFraNyLøsning(identer, 1)
    }

    @Test
    fun `siste 14a vedtak oppdateres ikke dersom bruker har nyere vedtak fra ny løsning som beholdes som gjeldende`() {
        val identer = gittBrukerIdenter()
        val fattetDato = now().minusDays(2)
        val referanse = UUID.randomUUID()

        val lagretVedtak = lagreFattetVedtak(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = Hovedmal.SKAFFE_ARBEID,
            vedtakFattetDato = fattetDato,
            referanse = referanse
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
            fraArena = false,
            vedtakId = Siste14aVedtak.VedtakIdVedtaksstotte(
                id = lagretVedtak.id,
                referanse = referanse
            )
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
        val lagretArenaVedtak = lagreArenaVedtak(
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
                fraArena = true,
                vedtakId = Siste14aVedtak.VedtakIdArena(
                    id = lagretArenaVedtak.vedtakId
                )
            )
        )
    }

    @Test
    fun `siste 14a vedtak oppdateres ikke dersom bruker har nyere vedtak fra Arena fra før med annet fnr`() {
        val identer = gittBrukerIdenter(antallHistoriskeFnr = 1)
        val fattetDato = LocalDate.now()

        val lagretArenaVedtak = lagreArenaVedtak(
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
                fraArena = true,
                vedtakId = Siste14aVedtak.VedtakIdArena(
                    id = lagretArenaVedtak.vedtakId
                )
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
            siste14aVedtakService.hentSiste14aVedtak(identer.fnr),
            "Siste 14a vedtak"
        )
    }

}

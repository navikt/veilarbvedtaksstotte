package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.client.pdl.PdlClient
import no.nav.common.featuretoggle.UnleashClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient
import no.nav.veilarbvedtaksstotte.client.dokument.DokumentSendtDTO
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaHovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.TestData.*
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toZonedDateTime
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random.Default.nextLong

class Siste14aVedtakServiceTest : DatabaseTest() {

    companion object {

        lateinit var arenaVedtakRepository: ArenaVedtakRepository
        lateinit var vedtakRepository: VedtaksstotteRepository

        val authService: AuthService = mock(AuthService::class.java)
        lateinit var arenaVedtakService: ArenaVedtakService
        lateinit var siste14aVedtakService: Siste14aVedtakService
        lateinit var unleashService: UnleashService

        val unleashClient = mock(UnleashClient::class.java)
        val pdlClient = mock(PdlClient::class.java)
        val aktorOppslagClient = mock(AktorOppslagClient::class.java)

        val kafkaProducerService = mock(KafkaProducerService::class.java)

        @BeforeClass
        @JvmStatic
        fun setup() {
            arenaVedtakRepository = ArenaVedtakRepository(jdbcTemplate)
            vedtakRepository = VedtaksstotteRepository(jdbcTemplate, transactor)

            unleashService = UnleashService(unleashClient)

            arenaVedtakService = ArenaVedtakService(arenaVedtakRepository, mock(SafClient::class.java), authService)
            siste14aVedtakService = Siste14aVedtakService(
                authService = authService,
                aktorOppslagClient = aktorOppslagClient,
                vedtakRepository = vedtakRepository,
                arenaVedtakRepository = arenaVedtakRepository,
                arenaVedtakService = arenaVedtakService,
                transactor = transactor,
                kafkaProducerService = kafkaProducerService
            )
        }
    }

    @Before
    fun before() {
        reset(pdlClient)
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

        val fattetDato = LocalDateTime.now()

        gittFattetVedtakDer(
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
        val fattetDato = LocalDateTime.now().minusDays(3)

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = LocalDate.now().minusDays(4),
                innsatsgruppe = ArenaInnsatsgruppe.VARIG,
                hovedmal = ArenaHovedmal.SKAFFEA
            )
        )

        gittFattetVedtakDer(
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
        val fattetDato = LocalDateTime.now().minusDays(3).plusMinutes(1)

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = LocalDate.now().minusDays(3),
                innsatsgruppe = ArenaInnsatsgruppe.VARIG,
                hovedmal = ArenaHovedmal.SKAFFEA,
                operationTimestamp = LocalDateTime.now().minusDays(3)
            )
        )

        gittFattetVedtakDer(
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

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = fattetDato,
                innsatsgruppe = ArenaInnsatsgruppe.BATT,
                hovedmal = ArenaHovedmal.OKEDELT
            )
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

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = fattetDato,
                innsatsgruppe = ArenaInnsatsgruppe.VARIG,
                hovedmal = ArenaHovedmal.SKAFFEA
            )
        )

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            vedtakFattetDato = LocalDateTime.now().minusDays(5)
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

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = fattetDato,
                innsatsgruppe = ArenaInnsatsgruppe.VARIG,
                hovedmal = ArenaHovedmal.SKAFFEA,
                operationTimestamp = LocalDateTime.now().minusDays(4).plusMinutes(1)
            )
        )

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            vedtakFattetDato = LocalDateTime.now().minusDays(4)
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

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = fattetDato,
                innsatsgruppe = ArenaInnsatsgruppe.VARIG,
                hovedmal = ArenaHovedmal.SKAFFEA
            )
        )

        identer.historiskeFnr.forEachIndexed { index, fnr ->
            lagre(
                arenaVedtakDer(
                    fnr = fnr,
                    fraDato = LocalDate.now().minusDays(5 + index.toLong()),
                    innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                    hovedmal = ArenaHovedmal.OKEDELT
                )
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

        lagre(
            arenaVedtakDer(
                fnr = identer.historiskeFnr[1],
                fraDato = fattetDato,
                innsatsgruppe = ArenaInnsatsgruppe.IKVAL,
                hovedmal = ArenaHovedmal.BEHOLDEA
            )
        )

        identer.historiskeFnr.forEachIndexed { index, fnr ->
            if (index != 1) {
                lagre(
                    arenaVedtakDer(
                        fnr = fnr,
                        fraDato = LocalDate.now().minusDays(5 + index.toLong()),
                        innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                        hovedmal = ArenaHovedmal.OKEDELT
                    )
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
            arenaVedtakDer(
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
    fun `siste 14a vedtak oppdateres ved melding om nytt vedtak fra Arena som er nyere enn vedtak fra ny løsning`() {
        val identer = gittBrukerIdenter()
        val fattetDato = LocalDate.now().minusDays(2)

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            vedtakFattetDato = LocalDateTime.now().minusDays(3)
        )

        assertAntallVedtakFraArena(identer, 0)
        assertFattedeVedtakFraNyLøsning(identer, 1)

        siste14aVedtakService.behandleEndringFraArena(
            arenaVedtakDer(
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
    }

    @Test
    fun `siste 14a vedtak oppdateres ikke dersom melding stammer fra ny løsning`() {
        val identer = gittBrukerIdenter()
        val fattetDato = LocalDateTime.now()

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            vedtakFattetDato = fattetDato
        )

        assertAntallVedtakFraArena(identer, 0)
        assertFattedeVedtakFraNyLøsning(identer, 1)

        siste14aVedtakService.behandleEndringFraArena(
            arenaVedtakDer(
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
    fun `siste 14a vedtak oppdateres ikke dersom bruker har nyere vedtak fra ny løsning`() {
        val identer = gittBrukerIdenter()
        val fattetDato = LocalDateTime.now().minusDays(2)

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = Hovedmal.SKAFFE_ARBEID,
            vedtakFattetDato = fattetDato
        )

        assertAntallVedtakFraArena(identer, 0)
        assertFattedeVedtakFraNyLøsning(identer, 1)

        siste14aVedtakService.behandleEndringFraArena(
            arenaVedtakDer(
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
    }

    @Test
    fun `siste 14a vedtak oppdateres ikke dersom bruker har nyere vedtak fra Arena fra før`() {
        val identer = gittBrukerIdenter()
        val fattetDato = LocalDate.now()
        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = fattetDato,
                innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                hovedmal = ArenaHovedmal.OKEDELT,
                hendelseId = 6
            )
        )

        assertAntallVedtakFraArena(identer, 1)

        siste14aVedtakService.behandleEndringFraArena(
            arenaVedtakDer(
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

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = fattetDato,
                innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                hovedmal = ArenaHovedmal.OKEDELT
            )
        )

        assertAntallVedtakFraArena(identer, 1)

        siste14aVedtakService.behandleEndringFraArena(
            arenaVedtakDer(
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
            "Antall vedtak fra ny løsning",
            antall,
            vedtakRepository.hentFattedeVedtak(identer.aktorId.get()).size
        )
    }

    fun assertAntallVedtakFraArena(identer: BrukerIdenter, antall: Int) {
        assertEquals(
            "Antall vedtak fra Arena",
            antall,
            arenaVedtakRepository.hentVedtakListe(identer.historiskeFnr.plus(identer.fnr)).size
        )
    }

    fun assertSiste14aVedtak(identer: BrukerIdenter, forventet: Siste14aVedtak?) {
        assertEquals(
            "Siste 14a vedtak",
            forventet,
            siste14aVedtakService.siste14aVedtak(identer.fnr)
        )
    }

    private fun gittBrukerIdenter(antallHistoriskeFnr: Int = 1): BrukerIdenter {
        val brukerIdenter = BrukerIdenter(
            Fnr(randomNumeric(10)),
            AktorId(randomNumeric(5)),
            (1..antallHistoriskeFnr).map { Fnr(randomNumeric(10)) },
            listOf()
        )

        `when`(aktorOppslagClient.hentIdenter(ArgumentMatchers.argThat { arg ->
            brukerIdenter.historiskeFnr
                .plus(brukerIdenter.historiskeAktorId)
                .plus(brukerIdenter.fnr)
                .plus(brukerIdenter.aktorId)
                .contains(arg)
        })).thenReturn(brukerIdenter)

        return brukerIdenter
    }

    private fun arenaVedtakDer(
        fnr: Fnr,
        fraDato: LocalDate = LocalDate.now(),
        regUser: String = "REG USER",
        innsatsgruppe: ArenaInnsatsgruppe = ArenaInnsatsgruppe.BFORM,
        hovedmal: ArenaHovedmal = ArenaHovedmal.SKAFFEA,
        operationTimestamp: LocalDateTime = LocalDateTime.now(),
        hendelseId: Long = nextLong()
    ): ArenaVedtak {
        val arenaVedtak = ArenaVedtak(
            fnr = fnr,
            innsatsgruppe = innsatsgruppe,
            hovedmal = hovedmal,
            fraDato = fraDato,
            regUser = regUser,
            operationTimestamp = operationTimestamp,
            hendelseId = hendelseId,
            vedtakId = 1
        )
        return arenaVedtak
    }


    private fun lagre(
        arenaVedtak: ArenaVedtak
    ): ArenaVedtak {

        arenaVedtakRepository.upsertVedtak(arenaVedtak)

        return arenaVedtak
    }

    private fun gittFattetVedtakDer(
        aktorId: AktorId,
        innsatsgruppe: Innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
        hovedmal: Hovedmal = Hovedmal.SKAFFE_ARBEID,
        vedtakFattetDato: LocalDateTime = LocalDateTime.now()
    ) {
        vedtakRepository.opprettUtkast(aktorId.get(), TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID)
        val vedtak = vedtakRepository.hentUtkast(aktorId.get())
        vedtak.innsatsgruppe = innsatsgruppe
        vedtak.hovedmal = hovedmal
        vedtakRepository.oppdaterUtkast(vedtak.id, vedtak)
        vedtakRepository.ferdigstillVedtak(vedtak.id, DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID))
        jdbcTemplate.update("UPDATE VEDTAK SET VEDTAK_FATTET = ? WHERE ID = ?", vedtakFattetDato, vedtak.id)
    }
}

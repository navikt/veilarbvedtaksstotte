package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.pdl.PdlClient
import no.nav.common.featuretoggle.UnleashClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient
import no.nav.veilarbvedtaksstotte.client.dokument.DokumentSendtDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.domain.BrukerIdenter
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaHovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.BrukerIdentService.HentIdentVariables
import no.nav.veilarbvedtaksstotte.service.BrukerIdentService.HentIdenterResponse
import no.nav.veilarbvedtaksstotte.service.BrukerIdentService.HentIdenterResponse.HentIdenterResponseData
import no.nav.veilarbvedtaksstotte.service.BrukerIdentService.HentIdenterResponse.HentIdenterResponseData.IdenterResponseData
import no.nav.veilarbvedtaksstotte.service.BrukerIdentService.HentIdenterResponse.HentIdenterResponseData.IdenterResponseData.IdentData
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer
import no.nav.veilarbvedtaksstotte.utils.TestData.*
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime

class InnsatsbehovServiceTest {

    companion object {
        lateinit var jdbcTemplate: JdbcTemplate
        lateinit var transactor: TransactionTemplate

        lateinit var arenaVedtakRepository: ArenaVedtakRepository
        lateinit var vedtakRepository: VedtaksstotteRepository

        val authService: AuthService = mock(AuthService::class.java)
        lateinit var arenaVedtakService: ArenaVedtakService
        lateinit var innsatsbehovService: InnsatsbehovService
        lateinit var unleashService: UnleashService
        lateinit var brukerIdentService: BrukerIdentService

        val veilarboppfolgingClient = mock(VeilarboppfolgingClient::class.java)
        val unleashClient = mock(UnleashClient::class.java)
        val pdlClient = mock(PdlClient::class.java)
        val aktorOppslagClient = mock(AktorOppslagClient::class.java)

        val kafkaProducerService = mock(KafkaProducerService::class.java)

        @BeforeClass
        @JvmStatic
        fun setup() {
            jdbcTemplate = SingletonPostgresContainer.init().createJdbcTemplate()
            transactor = TransactionTemplate(DataSourceTransactionManager(jdbcTemplate.dataSource!!))

            arenaVedtakRepository = ArenaVedtakRepository(jdbcTemplate)
            vedtakRepository = VedtaksstotteRepository(jdbcTemplate, transactor)

            unleashService = UnleashService(unleashClient)

            arenaVedtakService = ArenaVedtakService(arenaVedtakRepository, mock(SafClient::class.java), authService)
            brukerIdentService = BrukerIdentService(pdlClient, aktorOppslagClient, unleashService)
            innsatsbehovService = InnsatsbehovService(
                authService = authService,
                brukerIdentService = brukerIdentService,
                vedtakRepository = vedtakRepository,
                arenaVedtakRepository = arenaVedtakRepository,
                arenaVedtakService = arenaVedtakService,
                veilarboppfolgingClient = veilarboppfolgingClient,
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
    fun `innsatsbehov er null dersom, ny løsning har null, Arena har null`() {
        val identer = gittBrukerIdenter()
        assertAntallVedtakFraArena(identer, 0)
        assertFattedeVedtakFraNyLøsning(identer, 0)
        assertInnsatsbehov(identer, null)
    }

    @Test
    fun `innsatsbehov er null dersom, ny løsning har null, Arena har gamle utenfor gjeldende oppfølgingsperiode`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(2)),
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(1), null)
        )

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = LocalDate.now().minusDays(3)
            )
        )
        assertAntallVedtakFraArena(identer, 1)
        assertFattedeVedtakFraNyLøsning(identer, 0)
        assertInnsatsbehov(identer, null)
    }

    @Test
    fun `innsatsbehov er null dersom, ny løsning har ikke gjeldende, Arena har null`() {

        val identer = gittBrukerIdenter()

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            gjeldende = false
        )

        assertAntallVedtakFraArena(identer, 0)
        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertInnsatsbehov(identer, null)
    }

    @Test
    fun `innsatsbehov er null dersom, ny løsning har ikke gjeldende, Arena har gamle utenfor oppfølgingsperiode`() {

        val identer = gittBrukerIdenter(antallHistoriskeFnr = 1)

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(2)),
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(1), null)
        )

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = LocalDate.now().minusDays(3)
            )
        )

        lagre(
            arenaVedtakDer(
                fnr = identer.historiskeFnr[0],
                fraDato = LocalDate.now().minusDays(3)
            )
        )

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            gjeldende = false
        )

        assertAntallVedtakFraArena(identer, 2)
        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertGjeldendeVedtakNyLøsning(identer, false)
        assertInnsatsbehov(identer, null)
    }

    @Test
    fun `innsatsbehov fra ny løsning dersom, ny løsning har gjeldende, Arena har null`() {

        val identer = gittBrukerIdenter()

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.SKAFFE_ARBEID,
            gjeldende = true
        )

        assertAntallVedtakFraArena(identer, 0)
        assertGjeldendeVedtakNyLøsning(identer, true)
        assertInnsatsbehov(
            identer,
            Innsatsbehov(
                identer.aktorId, Innsatsgruppe.SPESIELT_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
            )
        )
    }

    @Test
    fun `innsatsbehov fra ny løsning dersom, ny løsning har gjeldende, Arena har gammelt utenfor oppfølgingsperiode`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(2)),
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(1), null)
        )

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = LocalDate.now().minusDays(3),
                innsatsgruppe = ArenaInnsatsgruppe.IKVAL
            )
        )

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            gjeldende = true
        )

        assertAntallVedtakFraArena(identer, 1)
        assertGjeldendeVedtakNyLøsning(identer, true)
        assertInnsatsbehov(
            identer,
            Innsatsbehov(
                identer.aktorId, Innsatsgruppe.SPESIELT_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID
            )
        )
    }

    @Test
    fun `innsatsbehov fra ny løsning dersom, ny løsning har gjeldende, Arena har eldre innenfor oppfølgingsperiode`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(10), null)
        )

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
            gjeldende = true,
            vedtakFattetDato = LocalDateTime.now().minusDays(3)
        )

        assertAntallVedtakFraArena(identer, 1)
        assertGjeldendeVedtakNyLøsning(identer, true)
        assertInnsatsbehov(
            identer,
            Innsatsbehov(
                identer.aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID
            )
        )
    }

    @Test
    fun `innsatsbehov fra ny løsning dersom, ny løsning har gjeldende, Arena har eldre fra samme dag`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(10), null)
        )

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
            gjeldende = true,
            vedtakFattetDato = LocalDateTime.now().minusDays(3).plusMinutes(1)
        )

        assertAntallVedtakFraArena(identer, 1)
        assertGjeldendeVedtakNyLøsning(identer, true)
        assertInnsatsbehov(
            identer,
            Innsatsbehov(
                identer.aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID
            )
        )
    }

    @Test
    fun `innsatsbehov er fra Arena dersom, ny løsning har null, Arena har innenfor oppfølgingsperiode`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(3)),
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(2), null)
        )

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = LocalDate.now().minusDays(1),
                innsatsgruppe = ArenaInnsatsgruppe.BATT,
                hovedmal = ArenaHovedmal.OKEDELT
            )
        )

        assertAntallVedtakFraArena(identer, 1)
        assertFattedeVedtakFraNyLøsning(identer, 0)
        assertInnsatsbehov(
            identer,
            Innsatsbehov(
                identer.aktorId, Innsatsgruppe.SPESIELT_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.OKE_DELTAKELSE
            )
        )

    }

    @Test
    fun `innsatsbehov er fra Arena dersom, ny løsning har ikke gjeldende, Arena har innenfor oppfølgingsperiode`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(5), null)
        )

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
            gjeldende = false
        )

        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertAntallVedtakFraArena(identer, 1)
        assertGjeldendeVedtakNyLøsning(identer, false)
        assertInnsatsbehov(
            identer,
            Innsatsbehov(
                identer.aktorId, Innsatsgruppe.VARIG_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
            )
        )
    }

    @Test
    fun `innsatsbehov er fra Arena dersom, ny løsning har gjeldende, Arena har nyere innenfor oppfølgingsperiode`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(5), null)
        )

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
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            gjeldende = true,
            vedtakFattetDato = LocalDateTime.now().minusDays(5)
        )

        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertAntallVedtakFraArena(identer, 1)
        assertGjeldendeVedtakNyLøsning(identer, true)
        assertInnsatsbehov(
            identer,
            Innsatsbehov(
                identer.aktorId, Innsatsgruppe.VARIG_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
            )
        )
    }

    @Test
    fun `innsatsbehov er fra Arena dersom, ny løsning har gjeldende, Arena har nyere fra samme dag`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(5), null)
        )

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = LocalDate.now().minusDays(4),
                innsatsgruppe = ArenaInnsatsgruppe.VARIG,
                hovedmal = ArenaHovedmal.SKAFFEA,
                operationTimestamp = LocalDateTime.now().minusDays(4).plusMinutes(1)
            )
        )

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            gjeldende = true,
            vedtakFattetDato = LocalDateTime.now().minusDays(4)
        )

        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertAntallVedtakFraArena(identer, 1)
        assertGjeldendeVedtakNyLøsning(identer, true)
        assertInnsatsbehov(
            identer,
            Innsatsbehov(
                identer.aktorId, Innsatsgruppe.VARIG_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
            )
        )
    }

    @Test
    fun `innsatsbehov er siste fra Arena dersom, ny løsning har null, Arena har også på historiske fnr innenfor samme oppfølgingsperiode`() {
        val identer = gittBrukerIdenter(antallHistoriskeFnr = 3)

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(10), null)
        )

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = LocalDate.now().minusDays(4),
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
        assertGjeldendeVedtakNyLøsning(identer, false)
        assertInnsatsbehov(
            identer,
            Innsatsbehov(
                identer.aktorId, Innsatsgruppe.VARIG_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
            )
        )
    }

    @Test
    fun `innsatsbehov er siste fra Arena dersom, ny løsning har null, Arena har bare på historiske fnr innenfor samme oppfølgingsperiode`() {
        val identer = gittBrukerIdenter(antallHistoriskeFnr = 4)

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(10), null)
        )

        lagre(
            arenaVedtakDer(
                fnr = identer.historiskeFnr[1],
                fraDato = LocalDate.now().minusDays(4),
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
        assertGjeldendeVedtakNyLøsning(identer, false)
        assertInnsatsbehov(
            identer,
            Innsatsbehov(
                identer.aktorId, Innsatsgruppe.STANDARD_INNSATS, HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID
            )
        )
    }

    @Test
    fun `innsatsbehov oppdateres ved melding om nytt vedtak fra Arena`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(5), null)
        )

        assertAntallVedtakFraArena(identer, 0)

        innsatsbehovService.behandleEndringFraArena(
            arenaVedtakDer(
                fnr = identer.fnr, innsatsgruppe = ArenaInnsatsgruppe.BFORM, hovedmal = ArenaHovedmal.OKEDELT
            )
        )

        val forventetInnsatsbehov = Innsatsbehov(
            identer.aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, HovedmalMedOkeDeltakelse.OKE_DELTAKELSE
        )

        verify(kafkaProducerService).sendInnsatsbehov(eq(forventetInnsatsbehov))

        assertInnsatsbehov(identer, forventetInnsatsbehov)
    }

    @Test
    fun `innsatsbehov oppdateres ved melding om nytt vedtak fra Arena og setter gjeldende fra ny løsning til false`() {
        val identer = gittBrukerIdenter()

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            gjeldende = true,
            vedtakFattetDato = LocalDateTime.now().minusDays(3)
        )

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(5), null)
        )

        assertAntallVedtakFraArena(identer, 0)
        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertGjeldendeVedtakNyLøsning(identer, true)

        innsatsbehovService.behandleEndringFraArena(
            arenaVedtakDer(
                fnr = identer.fnr,
                innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                hovedmal = ArenaHovedmal.OKEDELT,
                fraDato = LocalDate.now().minusDays(2)
            )
        )

        val forventetInnsatsbehov = Innsatsbehov(
            identer.aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, HovedmalMedOkeDeltakelse.OKE_DELTAKELSE
        )

        verify(kafkaProducerService).sendInnsatsbehov(eq(forventetInnsatsbehov))

        assertInnsatsbehov(identer, forventetInnsatsbehov)
        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertGjeldendeVedtakNyLøsning(identer, false)
    }

    @Test
    fun `innsatsbehov oppdateres ikke dersom melding stammer fra ny løsning`() {
        val identer = gittBrukerIdenter()

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            gjeldende = true
        )

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(5), null)
        )

        assertAntallVedtakFraArena(identer, 0)
        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertGjeldendeVedtakNyLøsning(identer, true)

        innsatsbehovService.behandleEndringFraArena(
            arenaVedtakDer(
                fnr = identer.fnr,
                innsatsgruppe = ArenaInnsatsgruppe.BATT,
                hovedmal = ArenaHovedmal.BEHOLDEA,
                regUser = "MODIA"
            )
        )

        val forventetInnsatsbehov = Innsatsbehov(
            identer.aktorId, Innsatsgruppe.SPESIELT_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID
        )

        verify(kafkaProducerService, never()).sendInnsatsbehov(any())

        assertInnsatsbehov(identer, forventetInnsatsbehov)
        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertGjeldendeVedtakNyLøsning(identer, true)
    }

    @Test
    fun `innsatsbehov oppdateres ikke dersom bruker har nyere vedtak fra ny løsning`() {
        val identer = gittBrukerIdenter()

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = Hovedmal.SKAFFE_ARBEID,
            gjeldende = true,
            vedtakFattetDato = LocalDateTime.now().minusDays(2)
        )

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(5), null)
        )

        assertAntallVedtakFraArena(identer, 0)
        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertGjeldendeVedtakNyLøsning(identer, true)

        innsatsbehovService.behandleEndringFraArena(
            arenaVedtakDer(
                fnr = identer.fnr,
                innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                hovedmal = ArenaHovedmal.BEHOLDEA,
                fraDato = LocalDate.now().minusDays(3)
            )
        )

        val forventetInnsatsbehov = Innsatsbehov(
            identer.aktorId, Innsatsgruppe.STANDARD_INNSATS, HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
        )

        verify(kafkaProducerService, never()).sendInnsatsbehov(any())

        assertInnsatsbehov(identer, forventetInnsatsbehov)
        assertFattedeVedtakFraNyLøsning(identer, 1)
        assertGjeldendeVedtakNyLøsning(identer, true)
    }

    @Test
    fun `innsatsbehov oppdateres ikke dersom bruker har nyere vedtak fra Arena fra før`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(5), null)
        )

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = LocalDate.now(),
                innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                hovedmal = ArenaHovedmal.OKEDELT
            )
        )

        assertAntallVedtakFraArena(identer, 1)

        innsatsbehovService.behandleEndringFraArena(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = LocalDate.now().minusDays(1),
                innsatsgruppe = ArenaInnsatsgruppe.IKVAL,
                hovedmal = ArenaHovedmal.SKAFFEA
            )
        )

        verify(kafkaProducerService, never()).sendInnsatsbehov(any())

        assertInnsatsbehov(
            identer, Innsatsbehov(
                identer.aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, HovedmalMedOkeDeltakelse.OKE_DELTAKELSE
            )
        )
    }

    @Test
    fun `innsatsbehov oppdateres ikke dersom bruker har nyere vedtak fra Arena fra før med annet fnr`() {
        val identer = gittBrukerIdenter(antallHistoriskeFnr = 1)

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(5), null)
        )

        lagre(
            arenaVedtakDer(
                fnr = identer.fnr,
                fraDato = LocalDate.now(),
                innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                hovedmal = ArenaHovedmal.OKEDELT
            )
        )

        assertAntallVedtakFraArena(identer, 1)

        innsatsbehovService.behandleEndringFraArena(
            arenaVedtakDer(
                fnr = identer.historiskeFnr[0],
                fraDato = LocalDate.now().minusDays(1),
                innsatsgruppe = ArenaInnsatsgruppe.IKVAL,
                hovedmal = ArenaHovedmal.SKAFFEA
            )
        )

        verify(kafkaProducerService, never()).sendInnsatsbehov(any())

        assertInnsatsbehov(
            identer, Innsatsbehov(
                identer.aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, HovedmalMedOkeDeltakelse.OKE_DELTAKELSE
            )
        )
    }

    @Test
    fun `innsatsbehov oppdateres ikke dersom samme melding har blitt behandlet før`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(5), null)
        )

        val arenaVedtak = arenaVedtakDer(
            fnr = identer.fnr,
            fraDato = LocalDate.now(),
            innsatsgruppe = ArenaInnsatsgruppe.BFORM,
            hovedmal = ArenaHovedmal.OKEDELT
        )
        lagre(arenaVedtak)

        assertAntallVedtakFraArena(identer, 1)

        innsatsbehovService.behandleEndringFraArena(arenaVedtak)

        assertInnsatsbehov(
            identer, Innsatsbehov(
                identer.aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, HovedmalMedOkeDeltakelse.OKE_DELTAKELSE
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

    fun assertGjeldendeVedtakNyLøsning(identer: BrukerIdenter, gjeldende: Boolean) {
        assertEquals(
            "Har lagret vedtak med gjeldende-flagg = true",
            gjeldende,
            vedtakRepository.hentGjeldendeVedtak(identer.aktorId.get()) != null
        )
    }

    fun assertInnsatsbehov(identer: BrukerIdenter, forventet: Innsatsbehov?) {
        assertEquals(
            "Innsatsbehov",
            forventet,
            innsatsbehovService.gjeldendeInnsatsbehov(identer.fnr)
        )
    }

    private fun gittBrukerIdenter(antallHistoriskeFnr: Int = 1): BrukerIdenter {
        val brukerIdenter = BrukerIdenter(
            fnr = Fnr(randomNumeric(10)),
            aktorId = AktorId(randomNumeric(5)),
            historiskeFnr = (1..antallHistoriskeFnr).map { Fnr(randomNumeric(10)) },
            historiskeAktorId = listOf()
        )

        val identerResponse = HentIdenterResponse(
            null,
            HentIdenterResponseData(
                IdenterResponseData(
                    brukerIdenter.historiskeFnr
                        .map { IdentData(it.get(), "FOLKEREGISTERIDENT", true) }
                        .plus(IdentData(brukerIdenter.fnr.get(), "FOLKEREGISTERIDENT", false))
                        .plus(IdentData(brukerIdenter.aktorId.get(), "AKTORID", false))
                )
            )
        )


        `when`(
            pdlClient.request(
                ArgumentMatchers.argThat { x ->
                    x.variables is HentIdentVariables &&
                            brukerIdenter.historiskeFnr
                                .plus(brukerIdenter.historiskeAktorId)
                                .plus(brukerIdenter.fnr)
                                .plus(brukerIdenter.aktorId)
                                .map { it.get() }
                                .contains((x.variables as HentIdentVariables).ident)
                },
                ArgumentMatchers.eq(HentIdenterResponse::class.java)
            )
        ).thenReturn(identerResponse)

        return brukerIdenter
    }

    private fun gittOppfolgingsperioder(identer: BrukerIdenter, vararg perioder: OppfolgingPeriodeDTO) {
        identer.historiskeFnr.plus(identer.fnr).forEach {
            `when`(veilarboppfolgingClient.hentOppfolgingsperioder(it.get())).thenReturn(perioder.asList())
        }
    }

    private fun arenaVedtakDer(
        fnr: Fnr,
        fraDato: LocalDate = LocalDate.now(),
        regUser: String = "REG USER",
        innsatsgruppe: ArenaInnsatsgruppe = ArenaInnsatsgruppe.BFORM,
        hovedmal: ArenaHovedmal = ArenaHovedmal.SKAFFEA,
        operationTimestamp: LocalDateTime = LocalDateTime.now(),
        hendelseId: Long = 12345
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
        gjeldende: Boolean,
        vedtakFattetDato: LocalDateTime = LocalDateTime.now()
    ) {
        vedtakRepository.opprettUtkast(aktorId.get(), TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID)
        val vedtak = vedtakRepository.hentUtkast(aktorId.get())
        vedtak.setInnsatsgruppe(innsatsgruppe)
        vedtak.setHovedmal(hovedmal)
        vedtakRepository.oppdaterUtkast(vedtak.id, vedtak)
        vedtakRepository.ferdigstillVedtak(vedtak.id, DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID))
        if (!gjeldende) {
            vedtakRepository.settGjeldendeVedtakTilHistorisk(aktorId.get())
        }
        jdbcTemplate.update("UPDATE VEDTAK SET VEDTAK_FATTET = ? WHERE ID = ?", vedtakFattetDato, vedtak.id)
    }

    private fun lagOppfolgingsperiode(start: LocalDateTime, slutt: LocalDateTime?): OppfolgingPeriodeDTO {
        val periode = OppfolgingPeriodeDTO()
        periode.setStartDato(start)
        periode.setSluttDato(slutt)
        return periode
    }
}

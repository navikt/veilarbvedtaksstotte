package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient
import no.nav.veilarbvedtaksstotte.client.dokument.DokumentSendtDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.domain.BrukerIdenter
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.*
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.kafka.KafkaProducer
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer
import no.nav.veilarbvedtaksstotte.utils.TestData.*
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
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

        // TODO ikke mock BrukerIdentService når den er implementert riktig
        val brukerIdentService: BrukerIdentService = mock(BrukerIdentService::class.java)
        val veilarboppfolgingClient: VeilarboppfolgingClient = mock(VeilarboppfolgingClient::class.java)
        val kafkaProducer = mock(KafkaProducer::class.java)

        @BeforeClass
        @JvmStatic
        fun setup() {
            jdbcTemplate = SingletonPostgresContainer.init().createJdbcTemplate()
            transactor = TransactionTemplate(DataSourceTransactionManager(jdbcTemplate.dataSource!!))

            arenaVedtakRepository = ArenaVedtakRepository(jdbcTemplate)
            vedtakRepository = VedtaksstotteRepository(jdbcTemplate, transactor)

            arenaVedtakService = ArenaVedtakService(arenaVedtakRepository, mock(SafClient::class.java), authService)

            innsatsbehovService = InnsatsbehovService(
                authService = authService,
                brukerIdentService = brukerIdentService,
                vedtakRepository = vedtakRepository,
                arenaVedtakRepository = arenaVedtakRepository,
                arenaVedtakService = arenaVedtakService,
                veilarboppfolgingClient = veilarboppfolgingClient,
                transactor = transactor,
                kafkaProducer = kafkaProducer
            )
        }
    }

    @Before
    fun before() {
        reset(kafkaProducer)
    }

    @Test
    fun `gjeldendeInnsatsbehov er null dersom det ikke finnes vedtak for bruker`() {
        val identer = gittBrukerIdenter()

        `verifiser gjeldende innsatsbehov før og etter opprydding og utsending på Kafka`(
            identer = identer,
            `forventet antall vedtak fra Arena før opprydding` = 0,
            `forventet gjeldende vedtak fra denne løsningen før opprydding` = false,
            `forventet vedtak fra Arena etter opprydding` = false,
            `forventet gjeldende vedtak fra denne løsningen etter opprydding` = false,
            `forventet innsatsbehov` = null
        )
    }

    @Test
    fun `gjeldendeInnsatsbehov er null dersom ingen gjeldende vedtak fra denne løsningen og Arena`() {

        val identer = gittBrukerIdenter(antallHistoriskeIdenter = 1)

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(2)),
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(1), null)
        )

        gittVedtakFraArenaDer(
            fnr = identer.fnr,
            fraDato = LocalDateTime.now().minusDays(3)
        )

        gittVedtakFraArenaDer(
            fnr = identer.historiskeFnr[0],
            fraDato = LocalDateTime.now().minusDays(3)
        )

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            gjeldende = false
        )

        `verifiser gjeldende innsatsbehov før og etter opprydding og utsending på Kafka`(
            identer = identer,
            `forventet antall vedtak fra Arena før opprydding` = 2,
            `forventet gjeldende vedtak fra denne løsningen før opprydding` = false,
            // Beholder siste kopi fra Arena
            `forventet vedtak fra Arena etter opprydding` = true,
            `forventet gjeldende vedtak fra denne løsningen etter opprydding` = false,
            `forventet innsatsbehov` = null
        )
    }

    @Test
    fun `gjeldendeInnsatsbehov er null dersom ingen gjeldende vedtak fra denne løsning og ingen fra Arena`() {

        val identer = gittBrukerIdenter()

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            gjeldende = false
        )

        `verifiser gjeldende innsatsbehov før og etter opprydding og utsending på Kafka`(
            identer = identer,
            `forventet antall vedtak fra Arena før opprydding` = 0,
            `forventet gjeldende vedtak fra denne løsningen før opprydding` = false,
            `forventet vedtak fra Arena etter opprydding` = false,
            `forventet gjeldende vedtak fra denne løsningen etter opprydding` = false,
            `forventet innsatsbehov` = null
        )
    }

    @Test
    fun `gjeldendeInnsatsbehov er null dersom ingen gjeldende vedtak fra Arena og ingen fra denne løsningen`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(2)),
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(1), null)
        )

        gittVedtakFraArenaDer(
            fnr = identer.fnr,
            fraDato = LocalDateTime.now().minusDays(3)
        )

        `verifiser gjeldende innsatsbehov før og etter opprydding og utsending på Kafka`(
            identer = identer,
            `forventet antall vedtak fra Arena før opprydding` = 1,
            `forventet gjeldende vedtak fra denne løsningen før opprydding` = false,
            // Beholder siste kopi fra Arena
            `forventet vedtak fra Arena etter opprydding` = true,
            `forventet gjeldende vedtak fra denne løsningen etter opprydding` = false,
            `forventet innsatsbehov` = null
        )
    }

    @Test
    fun `gjeldendeInnsatsbehov er fra Arena dersom innenfor oppfølgingsperiode og ingen vedtak i denne løsningen`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(3)),
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(2), null)
        )

        gittVedtakFraArenaDer(
            fnr = identer.fnr,
            fraDato = LocalDateTime.now().minusDays(1),
            innsatsgruppe = ArenaInnsatsgruppe.BATT,
            hovedmal = ArenaHovedmal.OKE_DELTAKELSE
        )

        `verifiser gjeldende innsatsbehov før og etter opprydding og utsending på Kafka`(
            identer = identer,
            `forventet antall vedtak fra Arena før opprydding` = 1,
            `forventet gjeldende vedtak fra denne løsningen før opprydding` = false,
            `forventet vedtak fra Arena etter opprydding` = true,
            `forventet gjeldende vedtak fra denne løsningen etter opprydding` = false,
            `forventet innsatsbehov` = Innsatsbehov(
                identer.aktorId, Innsatsgruppe.SPESIELT_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.OKE_DELTAKELSE
            )
        )
    }

    @Test
    fun `gjeldendeInnsatsbehov er fra Arena dersom innenfor oppfølgingsperiode og ikke gjeldende vedtak i denne løsningen`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(5), null)
        )

        gittVedtakFraArenaDer(
            fnr = identer.fnr,
            fraDato = LocalDateTime.now().minusDays(4),
            innsatsgruppe = ArenaInnsatsgruppe.VARIG,
            hovedmal = ArenaHovedmal.SKAFFE_ARBEID
        )

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            gjeldende = false
        )

        `verifiser gjeldende innsatsbehov før og etter opprydding og utsending på Kafka`(
            identer = identer,
            `forventet antall vedtak fra Arena før opprydding` = 1,
            `forventet gjeldende vedtak fra denne løsningen før opprydding` = false,
            `forventet vedtak fra Arena etter opprydding` = true,
            `forventet gjeldende vedtak fra denne løsningen etter opprydding` = false,
            `forventet innsatsbehov` = Innsatsbehov(
                identer.aktorId, Innsatsgruppe.VARIG_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
            )
        )
    }

    @Test
    fun `gjeldendeInnsatsbehov er fra Arena dersom innenfor oppfølgingsperiode og nyere enn gjeldende vedtak i denne løsningen`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(5), null)
        )

        gittVedtakFraArenaDer(
            fnr = identer.fnr,
            fraDato = LocalDateTime.now().minusDays(4),
            innsatsgruppe = ArenaInnsatsgruppe.VARIG,
            hovedmal = ArenaHovedmal.SKAFFE_ARBEID
        )

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            gjeldende = true,
            vedtakFattetDato = LocalDateTime.now().minusDays(5)
        )

        `verifiser gjeldende innsatsbehov før og etter opprydding og utsending på Kafka`(
            identer = identer,
            `forventet antall vedtak fra Arena før opprydding` = 1,
            `forventet gjeldende vedtak fra denne løsningen før opprydding` = true,
            `forventet vedtak fra Arena etter opprydding` = true,
            `forventet gjeldende vedtak fra denne løsningen etter opprydding` = false,
            `forventet innsatsbehov` = Innsatsbehov(
                identer.aktorId, Innsatsgruppe.VARIG_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
            )
        )
    }

    @Test
    fun `gjeldendeInnsatsbehov er siste fra Arena innenfor oppfølgingsperiode når ingen vedtak i denne løsningen`() {
        val identer = gittBrukerIdenter(antallHistoriskeIdenter = 3)

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(10), null)
        )

        gittVedtakFraArenaDer(
            fnr = identer.fnr,
            fraDato = LocalDateTime.now().minusDays(4),
            innsatsgruppe = ArenaInnsatsgruppe.VARIG,
            hovedmal = ArenaHovedmal.SKAFFE_ARBEID
        )

        identer.historiskeFnr.forEachIndexed { index, fnr ->
            gittVedtakFraArenaDer(
                fnr = fnr,
                fraDato = LocalDateTime.now().minusDays(5 + index.toLong()),
                innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                hovedmal = ArenaHovedmal.OKE_DELTAKELSE
            )
        }

        `verifiser gjeldende innsatsbehov før og etter opprydding og utsending på Kafka`(
            identer = identer,
            `forventet antall vedtak fra Arena før opprydding` = 4,
            `forventet gjeldende vedtak fra denne løsningen før opprydding` = false,
            `forventet vedtak fra Arena etter opprydding` = true,
            `forventet gjeldende vedtak fra denne løsningen etter opprydding` = false,
            `forventet innsatsbehov` = Innsatsbehov(
                identer.aktorId, Innsatsgruppe.VARIG_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
            )
        )
    }

    @Test
    fun `gjeldendeInnsatsbehov er siste fra Arena med gammelt fnr innenfor oppfølgingsperiode når ingen vedtak i denne løsningen`() {
        val identer = gittBrukerIdenter(antallHistoriskeIdenter = 4)

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(10), null)
        )

        gittVedtakFraArenaDer(
            fnr = identer.historiskeFnr[1],
            fraDato = LocalDateTime.now().minusDays(4),
            innsatsgruppe = ArenaInnsatsgruppe.IKVAL,
            hovedmal = ArenaHovedmal.BEHOLDE_ARBEID
        )

        identer.historiskeFnr.forEachIndexed { index, fnr ->
            if (index != 1) {
                gittVedtakFraArenaDer(
                    fnr = fnr,
                    fraDato = LocalDateTime.now().minusDays(5 + index.toLong()),
                    innsatsgruppe = ArenaInnsatsgruppe.BFORM,
                    hovedmal = ArenaHovedmal.OKE_DELTAKELSE
                )
            }
        }

        `verifiser gjeldende innsatsbehov før og etter opprydding og utsending på Kafka`(
            identer = identer,
            `forventet antall vedtak fra Arena før opprydding` = 4,
            `forventet gjeldende vedtak fra denne løsningen før opprydding` = false,
            `forventet vedtak fra Arena etter opprydding` = true,
            `forventet gjeldende vedtak fra denne løsningen etter opprydding` = false,
            `forventet innsatsbehov` = Innsatsbehov(
                identer.aktorId, Innsatsgruppe.STANDARD_INNSATS, HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID
            )
        )
    }

    @Test
    fun `gjeldendeInnsatsbehov fra denne løsningen dersom ingen vedtak fra Arena`() {

        val identer = gittBrukerIdenter()

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            hovedmal = Hovedmal.SKAFFE_ARBEID,
            gjeldende = true
        )

        `verifiser gjeldende innsatsbehov før og etter opprydding og utsending på Kafka`(
            identer = identer,
            `forventet antall vedtak fra Arena før opprydding` = 0,
            `forventet gjeldende vedtak fra denne løsningen før opprydding` = true,
            `forventet vedtak fra Arena etter opprydding` = false,
            `forventet gjeldende vedtak fra denne løsningen etter opprydding` = true,
            `forventet innsatsbehov` = Innsatsbehov(
                identer.aktorId, Innsatsgruppe.SPESIELT_TILPASSET_INNSATS, HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
            )
        )
    }

    @Test
    fun `gjeldendeInnsatsbehov er gjeldende fra denne løsningen dersom nyere enn vedtak fra Arena`() {
        val identer = gittBrukerIdenter()

        gittOppfolgingsperioder(
            identer,
            lagOppfolgingsperiode(LocalDateTime.now().minusDays(10), null)
        )

        gittVedtakFraArenaDer(
            fnr = identer.fnr,
            fraDato = LocalDateTime.now().minusDays(4),
            innsatsgruppe = ArenaInnsatsgruppe.VARIG,
            hovedmal = ArenaHovedmal.SKAFFE_ARBEID
        )

        gittFattetVedtakDer(
            aktorId = identer.aktorId,
            innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
            hovedmal = Hovedmal.BEHOLDE_ARBEID,
            gjeldende = true,
            vedtakFattetDato = LocalDateTime.now().minusDays(3)
        )

        `verifiser gjeldende innsatsbehov før og etter opprydding og utsending på Kafka`(
            identer = identer,
            `forventet antall vedtak fra Arena før opprydding` = 1,
            `forventet gjeldende vedtak fra denne løsningen før opprydding` = true,
            `forventet vedtak fra Arena etter opprydding` = false,
            `forventet gjeldende vedtak fra denne løsningen etter opprydding` = true,
            `forventet innsatsbehov` = Innsatsbehov(
                identer.aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID
            )
        )
    }

    fun `verifiser gjeldende innsatsbehov før og etter opprydding og utsending på Kafka`(
        identer: BrukerIdenter,
        `forventet antall vedtak fra Arena før opprydding`: Int,
        `forventet gjeldende vedtak fra denne løsningen før opprydding`: Boolean,
        `forventet vedtak fra Arena etter opprydding`: Boolean,
        `forventet gjeldende vedtak fra denne løsningen etter opprydding`: Boolean,
        `forventet innsatsbehov`: Innsatsbehov?
    ) {
        assertEquals(
            "Antall vedtak fra Arena før opprydding",
            `forventet antall vedtak fra Arena før opprydding`,
            arenaVedtakRepository.hentVedtakListe(identer.historiskeFnr.plus(identer.fnr)).size
        )
        assertEquals(
            "Har lagret vedtak med gjeldende-flagg = true",
            `forventet gjeldende vedtak fra denne løsningen før opprydding`,
            vedtakRepository.hentGjeldendeVedtak(identer.aktorId.get()) != null
        )

        assertEquals(
            "Innsatsbehov før opprydding",
            `forventet innsatsbehov`,
            innsatsbehovService.gjeldendeInnsatsbehov(identer.fnr)
        )

        innsatsbehovService.oppdaterInnsatsbehov(identer.fnr)

        verify(kafkaProducer).sendInnsatsbehov(eq(`forventet innsatsbehov`))

        assertEquals(
            "Har lagret vedtak fra Arena etter opprydding",
            if (`forventet vedtak fra Arena etter opprydding`) 1 else 0,
            arenaVedtakRepository.hentVedtakListe(identer.historiskeFnr.plus(identer.fnr)).size
        )
        assertEquals(
            "Har gjeldende vedtak fra denne løsningen etter opprydding",
            `forventet gjeldende vedtak fra denne løsningen etter opprydding`,
            vedtakRepository.hentGjeldendeVedtak(identer.aktorId.get()) != null
        )

        assertEquals(
            "Innsatsbehov etter opprydding",
            `forventet innsatsbehov`,
            innsatsbehovService.gjeldendeInnsatsbehov(identer.fnr)
        )
    }


    private fun gittBrukerIdenter(antallHistoriskeIdenter: Int = 1): BrukerIdenter {
        val brukerIdenter = BrukerIdenter(
            fnr = Fnr(randomNumeric(10)),
            aktorId = AktorId(randomNumeric(5)),
            historiskeFnr = (1..antallHistoriskeIdenter).map { Fnr(randomNumeric(10)) },
            historiskeAktorId = listOf()
        )

        `when`(brukerIdentService.hentIdenter(brukerIdenter.fnr)).thenReturn(brukerIdenter)
        `when`(brukerIdentService.hentIdenter(brukerIdenter.aktorId)).thenReturn(brukerIdenter)

        return brukerIdenter
    }

    private fun gittOppfolgingsperioder(identer: BrukerIdenter, vararg perioder: OppfolgingPeriodeDTO) {
        identer.historiskeFnr.plus(identer.fnr).forEach {
            `when`(veilarboppfolgingClient.hentOppfolgingsperioder(it.get())).thenReturn(perioder.asList())
        }
    }

    private fun gittVedtakFraArenaDer(
        fnr: Fnr,
        fraDato: LocalDateTime,
        regUser: String = "REG USER",
        innsatsgruppe: ArenaInnsatsgruppe = ArenaInnsatsgruppe.BFORM,
        hovedmal: ArenaHovedmal = ArenaHovedmal.SKAFFE_ARBEID
    ): ArenaVedtak {
        val arenaVedtak = ArenaVedtak(
            fnr = fnr,
            innsatsgruppe = innsatsgruppe,
            hovedmal = hovedmal,
            fraDato = fraDato,
            regUser = regUser
        )
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
        jdbcTemplate.update("UPDATE VEDTAK SET SIST_OPPDATERT = ? WHERE ID = ?", vedtakFattetDato, vedtak.id)
    }

    private fun lagOppfolgingsperiode(start: LocalDateTime, slutt: LocalDateTime?): OppfolgingPeriodeDTO {
        val periode = OppfolgingPeriodeDTO()
        periode.setStartDato(start)
        periode.setSluttDato(slutt)
        return periode
    }
}

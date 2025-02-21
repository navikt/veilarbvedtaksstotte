package no.nav.veilarbvedtaksstotte.service

import io.getunleash.DefaultUnleash
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.norg2.Enhet
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.utils.fn.UnsafeRunnable
import no.nav.poao_tilgang.client.Decision.Permit
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.api.ApiResult
import no.nav.veilarbvedtaksstotte.client.aiaBackend.AiaBackendClient
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EgenvurderingResponseDTO
import no.nav.veilarbvedtaksstotte.client.aiaBackend.request.EgenvurderingForPersonRequest
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.ArbeidssoekerRegisteretService
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient
import no.nav.veilarbvedtaksstotte.client.arena.dto.VeilarbArenaOppfolging
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.Journalpost
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.Journalpost.JournalpostDokument
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.JournalpostGraphqlResponse
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.JournalpostGraphqlResponse.JournalpostReponseData
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.OpprettJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.OpprettetJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.OpprettetJournalpostDTO.DokumentInfoId
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetKontaktinformasjon
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetStedsadresse
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.client.person.dto.CvDto.CVMedInnhold
import no.nav.veilarbvedtaksstotte.client.person.dto.CvInnhold
import no.nav.veilarbvedtaksstotte.client.person.dto.PersonNavn
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO.AdresseType
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingPeriodeDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.SakDTO
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.Veileder
import no.nav.veilarbvedtaksstotte.config.EnvironmentProperties
import no.nav.veilarbvedtaksstotte.controller.dto.OppdaterUtkastDTO
import no.nav.veilarbvedtaksstotte.domain.Målform
import no.nav.veilarbvedtaksstotte.domain.VedtakOpplysningKilder
import no.nav.veilarbvedtaksstotte.domain.arkiv.BrevKode
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingMetode
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingStatus
import no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.*
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils
import no.nav.veilarbvedtaksstotte.utils.JsonUtils.fromJson
import no.nav.veilarbvedtaksstotte.utils.SAK_STATISTIKK_PAA
import no.nav.veilarbvedtaksstotte.utils.TestData
import no.nav.veilarbvedtaksstotte.utils.TestUtils.readTestResourceFile
import org.junit.jupiter.api.*
import org.mockito.kotlin.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SakStatistikkServiceTest : DatabaseTest() {

    companion object {
        private var vedtaksstotteRepository: VedtaksstotteRepository? = null
        private var kilderRepository: KilderRepository? = null
        private var meldingRepository: MeldingRepository? = null
        private var sakStatistikkRepository: SakStatistikkRepository? = null
        private var vedtakService: VedtakService? = null
        private var oyeblikksbildeService: OyeblikksbildeService? = null
        private var authService: AuthService? = null
        private var sakStatistikkService: SakStatistikkService? = null
        private val unleashClient: DefaultUnleash = mock()
        private val leaderElectionClient: LeaderElectionClient = mock()
        private val vedtakHendelserService: VedtakHendelserService = mock()
        private val veilederService: VeilederService = mock()
        private val veilarbpersonClient: VeilarbpersonClient = mock()
        private val arbeidssoekerRegistretService: ArbeidssoekerRegisteretService = mock()
        private val aia_backend_client: AiaBackendClient = mock()
        private val regoppslagClient: RegoppslagClient = mock()
        private val aktorOppslagClient: AktorOppslagClient = mock()
        private val veilarbarenaClient: VeilarbarenaClient = mock()
        private val dokarkivClient: DokarkivClient = mock()
        private val veilarbveilederClient: VeilarbveilederClient = mock()
        private val utrullingService: UtrullingService = mock()
        private val enhetInfoService: EnhetInfoService = mock()
        private val safClient: SafClient = mock()
        private val metricsService: MetricsService = mock()
        private val poaoTilgangClient: PoaoTilgangClient = mock()
        private val pdfService: PdfService = mock()
        private val veilarboppfolgingClient: VeilarboppfolgingClient = mock()
        private val bigQueryService: BigQueryService = mock()
        private val environmentProperties: EnvironmentProperties = mock()
        private val sakStatistikkService2: SakStatistikkService = mock()

        @JvmStatic
        @BeforeAll
        fun setupOnce() {
            val veilarbarenaService = VeilarbarenaService(veilarbarenaClient)
            kilderRepository = spy(KilderRepository(jdbcTemplate))
            meldingRepository = spy(MeldingRepository(jdbcTemplate))
            vedtaksstotteRepository = VedtaksstotteRepository(jdbcTemplate, transactor)

            val oyeblikksbildeRepository = OyeblikksbildeRepository(jdbcTemplate)
            val beslutteroversiktRepository = BeslutteroversiktRepository(jdbcTemplate)
            authService = spy(
                AuthService(
                    aktorOppslagClient,
                    veilarbarenaService,
                    AuthContextHolderThreadLocal.instance(),
                    utrullingService,
                    poaoTilgangClient
                )
            )

            oyeblikksbildeService = OyeblikksbildeService(
                authService,
                oyeblikksbildeRepository,
                vedtaksstotteRepository,
                veilarbpersonClient,
                aia_backend_client,
                arbeidssoekerRegistretService
            )
            val malTypeService = MalTypeService(arbeidssoekerRegistretService)
            val dokumentService = DokumentService(
                regoppslagClient,
                veilarboppfolgingClient,
                veilarbpersonClient,
                dokarkivClient,
                malTypeService,
                oyeblikksbildeService!!,
                pdfService
            )
            vedtakService = VedtakService(
                transactor,
                vedtaksstotteRepository,
                beslutteroversiktRepository,
                kilderRepository,
                meldingRepository,
                safClient,
                authService,
                oyeblikksbildeService,
                veilederService,
                vedtakHendelserService,
                dokumentService,
                veilarbarenaService,
                metricsService,
                leaderElectionClient,
                sakStatistikkService2
            )

        }
    }

    @BeforeEach
    fun setup() {

        DbTestUtils.cleanupDb(jdbcTemplate)
        sakStatistikkRepository = SakStatistikkRepository(jdbcTemplate)
        reset(veilederService, meldingRepository, unleashClient, dokarkivClient, vedtakHendelserService)
        doReturn(TestData.TEST_VEILEDER_IDENT).`when`(authService)?.innloggetVeilederIdent
        doReturn(UUID.randomUUID()).`when`(authService)?.hentInnloggetVeilederUUID()

        whenever(veilederService.hentEnhetNavn(TestData.TEST_OPPFOLGINGSENHET_ID)).thenReturn(TestData.TEST_OPPFOLGINGSENHET_NAVN)
        whenever(veilederService.hentVeileder(TestData.TEST_VEILEDER_IDENT)).thenReturn(
            Veileder(
                TestData.TEST_VEILEDER_IDENT, TestData.TEST_VEILEDER_NAVN
            )
        )
        whenever(veilederService.hentVeilederEllerNull(TestData.TEST_VEILEDER_IDENT)).thenReturn(
            Optional.of(
                Veileder(
                    TestData.TEST_VEILEDER_IDENT, TestData.TEST_VEILEDER_NAVN
                )
            )
        )

        whenever(regoppslagClient.hentPostadresse(any())).thenReturn(
            RegoppslagResponseDTO(
                "", RegoppslagResponseDTO.Adresse(AdresseType.NORSKPOSTADRESSE, "", "", "", "", "", "", "")
            )
        )

        whenever(veilarbpersonClient.hentMålform(TestData.TEST_FNR)).thenReturn(Målform.NB)
        whenever(veilarbpersonClient.hentCVOgJobbprofil(TestData.TEST_FNR.get())).thenReturn(
            CVMedInnhold(
                fromJson(
                    testCvData(), CvInnhold::class.java
                )
            )
        )
        whenever(veilarbpersonClient.hentPersonNavn(TestData.TEST_FNR.get())).thenReturn(
            PersonNavn(
                "Fornavn", null, "Etternavn", null
            )
        )

        whenever(aia_backend_client.hentEgenvurdering(any<EgenvurderingForPersonRequest>())).thenReturn(
            fromJson(
                testEgenvurderingData(), EgenvurderingResponseDTO::class.java
            )
        )

        whenever(aktorOppslagClient.hentAktorId(TestData.TEST_FNR)).thenReturn(AktorId.of(TestData.TEST_AKTOR_ID))
        whenever(aktorOppslagClient.hentFnr(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(TestData.TEST_FNR)

        whenever(veilarbarenaClient.hentOppfolgingsbruker(TestData.TEST_FNR)).thenReturn(
            Optional.of(
                VeilarbArenaOppfolging(TestData.TEST_OPPFOLGINGSENHET_ID, "ARBS", "IKVAL")
            )
        )
        whenever(dokarkivClient.opprettJournalpost(any<OpprettJournalpostDTO>())).thenReturn(
            OpprettetJournalpostDTO(
                TestData.TEST_JOURNALPOST_ID, true, listOf(DokumentInfoId(TestData.TEST_DOKUMENT_ID))
            )
        )
        whenever(veilarbveilederClient.hentVeileder(TestData.TEST_VEILEDER_IDENT)).thenReturn(
            Veileder(
                TestData.TEST_VEILEDER_IDENT, TestData.TEST_VEILEDER_NAVN
            )
        )

        whenever(enhetInfoService.hentEnhet(EnhetId.of(TestData.TEST_OPPFOLGINGSENHET_ID))).thenReturn(
            Enhet().setNavn(
                TestData.TEST_OPPFOLGINGSENHET_NAVN
            )
        )
        whenever(enhetInfoService.utledEnhetKontaktinformasjon(any<EnhetId>())).thenReturn(
            EnhetKontaktinformasjon(
                EnhetId.of(TestData.TEST_OPPFOLGINGSENHET_ID), EnhetStedsadresse("", "", "", "", "", ""), ""
            )
        )

        whenever(pdfService.produserDokument(any())).thenReturn(byteArrayOf())
        whenever(pdfService.produserCVPdf(any())).thenReturn(Optional.of(byteArrayOf()))
        whenever(pdfService.produserBehovsvurderingPdf(any())).thenReturn(Optional.of(byteArrayOf()))
        whenever(poaoTilgangClient.evaluatePolicy(any())).thenReturn(ApiResult(null, Permit))
        whenever(safClient.hentJournalpost(any())).thenReturn(mockedJournalpostGraphqlResponse)
        whenever(unleashClient.isEnabled(SAK_STATISTIKK_PAA)).thenReturn(true)
        whenever(veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(TestData.TEST_FNR)).thenReturn(
            mockedOppfolgingsPeriode
        )
        whenever(veilarboppfolgingClient.hentOppfolgingsperiodeSak(any())).thenReturn(mockedOppfolgingsSak)
        whenever(environmentProperties.naisAppImage).thenReturn("naisAppImage")

        sakStatistikkService = SakStatistikkService(
            sakStatistikkRepository!!,
            veilarboppfolgingClient,
            aktorOppslagClient,
            bigQueryService,
            unleashClient,
            environmentProperties
        )
    }

    @Test
    fun legg_til_statisitkkrad_fattet_vedtak() {

        withContext {
            gittUtkastKlarForUtsendelse()
            fattVedtak()
            val vedtaket = hentVedtak()
            sakStatistikkService!!.fattetVedtak(vedtaket, TestData.TEST_FNR)
            val statistikkListe = sakStatistikkRepository!!.hentSakStatistikkListe(TestData.TEST_AKTOR_ID)
            val lagretRad = statistikkListe.last()
            Assertions.assertEquals(
                BehandlingStatus.FATTET, lagretRad.behandlingStatus, "Behandling status skal være FATTET"
            )
            Assertions.assertNotNull(
                lagretRad.behandlingResultat, "Behandling resultat skal finnes"
            )
            Assertions.assertNotNull(
                lagretRad.mottattTid, "Mottatt tid skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.registrertTid, "Registrert tid skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.ferdigbehandletTid, "Ferdigbehandlet tid skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.endretTid, "Endret tid skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.tekniskTid, "Teknisk tid skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.opprettetAv, "Opprettet av skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.saksbehandler, "Saksbehandler skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.ansvarligEnhet, "Ansvarlig enhet skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.behandlingMetode, "Behandlingsmetode skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.avsender, "Avsender skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.versjon, "Versjon skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.oppfolgingPeriodeUUID, "OppfolgingsperiodeUUID skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.innsatsgruppe, "Innsatsgruppe skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.hovedmal, "Hovedmål av skal være utfylt"
            )
        }
    }

    @Test
    fun legg_til_statistikkrad_naar_utkast_er_opprettet() {

        withContext {
            gittUtkastKlarForUtsendelse()
            sakStatistikkService!!.opprettetUtkast(
                vedtaksstotteRepository!!.hentUtkast(TestData.TEST_AKTOR_ID), TestData.TEST_FNR
            )
            val statistikkListe = sakStatistikkRepository!!.hentSakStatistikkListe(TestData.TEST_AKTOR_ID)
            val lagretRad = statistikkListe.last()
            Assertions.assertEquals(
                BehandlingStatus.UNDER_BEHANDLING,
                lagretRad.behandlingStatus,
                "Behandling status skal være UNDER_BEHANDLING"
            )
            Assertions.assertNotNull(
                lagretRad.mottattTid, "Mottatt tid skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.registrertTid, "Registrert tid skal være utfylt"
            )
            Assertions.assertNull(
                lagretRad.ferdigbehandletTid, "Ferdigbehandlet tid skal være null"
            )
            Assertions.assertNotNull(
                lagretRad.endretTid, "Endret tid skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.tekniskTid, "Teknisk tid skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.opprettetAv, "Opprettet av skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.saksbehandler, "Saksbehandler skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.ansvarligEnhet, "Ansvarlig enhet skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.behandlingMetode, "Behandlingsmetode skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.avsender, "Avsender skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.versjon, "Versjon skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.oppfolgingPeriodeUUID, "OppfolgingsperiodeUUID skal være utfylt"
            )
        }
    }

    @Test
    fun legg_til_statistikkrad_naar_utkast_slettes() {

        withContext {
            gittUtkastKlarForUtsendelse()

            val utkastet = vedtaksstotteRepository!!.hentUtkast(TestData.TEST_AKTOR_ID)
            vedtakService!!.slettUtkast(utkastet)
            sakStatistikkService!!.slettetUtkast(utkastet)
            val statistikkListe = sakStatistikkRepository!!.hentSakStatistikkListe(TestData.TEST_AKTOR_ID)

            val lagretRad = statistikkListe.last()
            Assertions.assertEquals(
                BehandlingStatus.AVBRUTT, lagretRad.behandlingStatus, "Behandling status skal være AVBRUTT"
            )
            Assertions.assertNull(
                lagretRad.behandlingResultat, "Behandling resultat skal være null"
            )
            Assertions.assertNotNull(
                lagretRad.mottattTid, "Mottatt tid skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.registrertTid, "Registrert tid skal være utfylt"
            )
            Assertions.assertNull(
                lagretRad.ferdigbehandletTid, "Ferdigbehandlet tid skal være null"
            )
            Assertions.assertNotNull(
                lagretRad.endretTid, "Endret tid skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.tekniskTid, "Teknisk tid skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.opprettetAv, "Opprettet av skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.saksbehandler, "Saksbehandler skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.ansvarligEnhet, "Ansvarlig enhet skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.behandlingMetode, "Behandlingsmetode skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.avsender, "Avsender skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.versjon, "Versjon skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.oppfolgingPeriodeUUID, "OppfolgingsperiodeUUID skal være utfylt"
            )
        }
    }

    @Test
    fun legg_til_statistikkrad_utkast_som_er_revurdering() {
        withContext {

            gittUtkastKlarForUtsendelse()
            fattVedtak()
            val vedtaket = hentVedtak()
            sakStatistikkService!!.fattetVedtak(vedtaket, TestData.TEST_FNR)
            vedtakService!!.lagUtkast(TestData.TEST_FNR)
            val nestSisteRad = sakStatistikkRepository!!.hentSakStatistikkListe(TestData.TEST_AKTOR_ID).last()
            sakStatistikkService!!.opprettetUtkast(
                vedtaksstotteRepository!!.hentUtkast(TestData.TEST_AKTOR_ID), TestData.TEST_FNR
            )
            val statistikkListe = sakStatistikkRepository!!.hentSakStatistikkListe(TestData.TEST_AKTOR_ID)
            val lagretRad = statistikkListe.last()
            Assertions.assertEquals(
                BehandlingStatus.UNDER_BEHANDLING,
                lagretRad.behandlingStatus,
                "Behandling status skal være UNDER_BEHANDLING"
            )
            Assertions.assertEquals(
                lagretRad.relatertBehandlingId,
                nestSisteRad.behandlingId,
                "Relatert behandling id skal være lik nest siste rad sin behandling id"
            )
            Assertions.assertNull(
                lagretRad.behandlingResultat, "Behandling resultat skal være null"
            )
            Assertions.assertNotNull(
                lagretRad.mottattTid, "Mottatt tid skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.registrertTid, "Registrert tid skal være utfylt"
            )
            Assertions.assertNull(
                lagretRad.ferdigbehandletTid, "Ferdigbehandlet tid skal være null"
            )
            Assertions.assertNotNull(
                lagretRad.endretTid, "Endret tid skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.tekniskTid, "Teknisk tid skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.opprettetAv, "Opprettet av skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.saksbehandler, "Saksbehandler skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.ansvarligEnhet, "Ansvarlig enhet skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.behandlingMetode, "Behandlingsmetode skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.avsender, "Avsender skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.versjon, "Versjon skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.oppfolgingPeriodeUUID, "OppfolgingsperiodeUUID skal være utfylt"
            )
        }
    }

    @Test
    fun legg_til_statistikkrad_totrinns_og_til_kvalitetssikrer() {
        //Legg til statistikkrad som er totrinns behandling
        //Legg til statistikkrad som er sendt til kvalitetssikrer
        withContext {
            gittUtkastKlarForUtsendelse()
            sakStatistikkService!!.opprettetUtkast(
                vedtaksstotteRepository!!.hentUtkast(TestData.TEST_AKTOR_ID), TestData.TEST_FNR
            )
            var statistikkListe = sakStatistikkRepository!!.hentSakStatistikkListe(TestData.TEST_AKTOR_ID)
            val behandlingsId = statistikkListe.last().behandlingId?.toLong()
            val oppdaterDto = OppdaterUtkastDTO().setHovedmal(Hovedmal.SKAFFE_ARBEID).setBegrunnelse("En begrunnelse")
                .setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS).setOpplysninger(
                    listOf(
                        VedtakOpplysningKilder.REGISTRERING.desc,
                        VedtakOpplysningKilder.EGENVURDERING.desc,
                        VedtakOpplysningKilder.CV.desc,
                        VedtakOpplysningKilder.ARBEIDSSOKERREGISTERET.desc
                    )
                )
            vedtakService!!.oppdaterUtkast(
                behandlingsId!!, oppdaterDto
            )
            val utkast = vedtaksstotteRepository!!.hentUtkast(TestData.TEST_AKTOR_ID)
            utkast.setBeslutterProsessStatus(BeslutterProsessStatus.GODKJENT_AV_BESLUTTER)
            vedtaksstotteRepository!!.oppdaterUtkast(utkast.id, utkast)
            sakStatistikkService!!.startetKvalitetssikring(vedtaksstotteRepository!!.hentVedtak(utkast.id))
            sakStatistikkService!!.bliEllerTaOverSomKvalitetssikrer(
                vedtaksstotteRepository!!.hentVedtak(utkast.id), TestData.TEST_BESLUTTER_IDENT
            )
            statistikkListe = sakStatistikkRepository!!.hentSakStatistikkListe(TestData.TEST_AKTOR_ID)


            val lagretRad = statistikkListe.last()
            Assertions.assertNull(
                lagretRad.ferdigbehandletTid, "Ferdigbehandlet tid skal ikke være utfylt"
            )
            Assertions.assertEquals(
                BehandlingStatus.SENDT_TIL_KVALITETSSIKRING,
                lagretRad.behandlingStatus,
                "Behandling status skal være SENDT_TIL_KVALITETSSIKRING"
            )
            Assertions.assertEquals(
                BehandlingMetode.TOTRINNS, lagretRad.behandlingMetode, "Behandling metode skal være TOTRINNS"
            )
            Assertions.assertEquals(
                lagretRad.ansvarligBeslutter,
                TestData.TEST_BESLUTTER_IDENT,
                "Ansvarlig beslutter  skal være lik TEST_BESLUTTER_IDENT"
            )
        }
    }

    @Test
    fun legg_til_statistikkrad_for_a_overta_som_kvalitetssikrer() {
        withContext {
            gittUtkastKlarForUtsendelse()
            var utkastet = vedtaksstotteRepository!!.hentUtkast(TestData.TEST_AKTOR_ID)
            vedtaksstotteRepository!!.setBeslutter(utkastet.id, TestData.TEST_BESLUTTER_IDENT_2)
            utkastet = vedtaksstotteRepository!!.hentUtkast(TestData.TEST_AKTOR_ID)
            vedtaksstotteRepository!!.oppdaterUtkast(utkastet.id, utkastet)
            sakStatistikkService!!.bliEllerTaOverSomKvalitetssikrer(
                vedtaksstotteRepository!!.hentVedtak(utkastet.id), TestData.TEST_BESLUTTER_IDENT_2
            )
            val statistikkListe = sakStatistikkRepository!!.hentSakStatistikkListe(TestData.TEST_AKTOR_ID)
            val lagretRad = statistikkListe.last()
            Assertions.assertNull(
                lagretRad.ferdigbehandletTid, "Ferdigbehandlet tid skal ikke være utfylt"
            )
            Assertions.assertEquals(
                BehandlingStatus.SENDT_TIL_KVALITETSSIKRING,
                lagretRad.behandlingStatus,
                "Behandling status skal være SENDT_TIL_KVALITETSSIKRING"
            )
            Assertions.assertEquals(
                BehandlingMetode.TOTRINNS, lagretRad.behandlingMetode, "Behandling metode skal være TOTRINNS"
            )
            Assertions.assertEquals(
                lagretRad.ansvarligBeslutter,
                TestData.TEST_BESLUTTER_IDENT_2,
                "Ansvarlig beslutter skal være lik TEST_BESLUTTER_IDENT_2"
            )
        }
    }

    @Test
    fun legg_til_statistikkrad_for_a_overta_utkast() {
        sakStatistikkRepository = SakStatistikkRepository(jdbcTemplate)
        withContext {
            gittUtkastKlarForUtsendelse()
            var utkast = vedtaksstotteRepository!!.hentUtkast(TestData.TEST_AKTOR_ID)
            vedtaksstotteRepository!!.oppdaterUtkastVeileder(utkast.id, TestData.TEST_VEILEDER_IDENT_2)
            utkast = vedtaksstotteRepository!!.hentUtkast(TestData.TEST_AKTOR_ID)
            utkast.setBeslutterProsessStatus(BeslutterProsessStatus.GODKJENT_AV_BESLUTTER)
            vedtaksstotteRepository!!.oppdaterUtkast(utkast.id, utkast)

            sakStatistikkService!!.overtattUtkast(
                vedtaksstotteRepository!!.hentVedtak(utkast.id), TestData.TEST_VEILEDER_IDENT_2, false
            )

            val statistikkListe = sakStatistikkRepository!!.hentSakStatistikkListe(TestData.TEST_AKTOR_ID)
            val lagretRad = statistikkListe.last()
            Assertions.assertNull(
                lagretRad.ferdigbehandletTid, "Ferdigbehandlet tid skal ikke være utfylt"
            )
            Assertions.assertEquals(
                lagretRad.saksbehandler,
                TestData.TEST_VEILEDER_IDENT_2,
                "Saksbehandler skal være lik TEST_VEILEDER_IDENT_2"
            )
        }
    }

    private fun gittTilgang() {
        whenever(utrullingService.erUtrullet(any())).thenReturn(true)
        whenever(poaoTilgangClient.evaluatePolicy(any())).thenReturn(ApiResult(null, Permit))
    }

    private fun withContext(runnable: UnsafeRunnable) {
        AuthContextHolderThreadLocal.instance()
            .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, TestData.TEST_VEILEDER_IDENT), runnable)
    }

    private fun gittUtkastKlarForUtsendelse() {
        withContext {
            gittTilgang()
            vedtakService!!.lagUtkast(TestData.TEST_FNR)
            val utkast = vedtaksstotteRepository!!.hentUtkast(TestData.TEST_AKTOR_ID)

            val oppdaterDto = OppdaterUtkastDTO().setHovedmal(Hovedmal.SKAFFE_ARBEID).setBegrunnelse("En begrunnelse")
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS).setOpplysninger(
                    listOf(
                        VedtakOpplysningKilder.CV.desc,
                        VedtakOpplysningKilder.EGENVURDERING.desc,
                        VedtakOpplysningKilder.ARBEIDSSOKERREGISTERET.desc
                    )
                )

            val kilder = listOf(
                "CV-en/jobbønskene dine på nav.no",
                "Svarene dine om behov for veiledning",
                "Svarene dine fra da du registrerte deg"
            )

            kilderRepository!!.lagKilder(kilder, utkast.id)
            vedtakService!!.oppdaterUtkast(utkast.id, oppdaterDto)
        }
    }

    private fun hentVedtak(): Vedtak {
        val vedtakList = vedtakService!!.hentFattedeVedtak(TestData.TEST_FNR)
        Assertions.assertEquals(vedtakList.size, 1)
        return vedtakList[0]
    }

    private fun fattVedtak() {
        withContext {
            gittTilgang()
            val utkast = vedtaksstotteRepository!!.hentUtkast(TestData.TEST_AKTOR_ID)
            vedtakService!!.fattVedtak(utkast.id)
        }
    }

    private fun testCvData(): String {
        return readTestResourceFile("testdata/oyeblikksbilde-cv.json")
    }

    private fun testEgenvurderingData(): String {
        return readTestResourceFile("testdata/egenvurdering-response.json")
    }

    private val mockedJournalpostGraphqlResponse: JournalpostGraphqlResponse
        get() {
            val journalpostGraphqlResponse = JournalpostGraphqlResponse()
            journalpostGraphqlResponse.data = JournalpostReponseData().setJournalpost(mockedJournalpost)
            return journalpostGraphqlResponse
        }

    private val mockedJournalpost: Journalpost
        get() {
            val journalpost = Journalpost()
            journalpost.journalpostId = "journalpost123"
            journalpost.tittel = "titel"

            val journalpostDokument1 = JournalpostDokument()
            journalpostDokument1.brevkode = BrevKode.EGENVURDERING.name
            journalpostDokument1.dokumentInfoId = "111111"

            val journalpostDokument2 = JournalpostDokument()
            journalpostDokument2.brevkode = BrevKode.REGISTRERINGSINFO.name
            journalpostDokument2.dokumentInfoId = "222222"

            val journalpostDokument3 = JournalpostDokument()
            journalpostDokument3.brevkode = BrevKode.CV_OG_JOBBPROFIL.name
            journalpostDokument3.dokumentInfoId = "333333"


            journalpost.dokumenter = arrayOf(journalpostDokument1, journalpostDokument2, journalpostDokument3)
            return journalpost
        }

    private val mockedOppfolgingsPeriode: Optional<OppfolgingPeriodeDTO>
        get() {
            val oppfolgingsPeriode = OppfolgingPeriodeDTO()
            oppfolgingsPeriode.uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
            oppfolgingsPeriode.startDato = ZonedDateTime.of(
                2025, 1, 4, 9, 48, 58, 0, ZoneId.of("+1")
            ).plus(762, ChronoUnit.MILLIS)
            return Optional.of(oppfolgingsPeriode)
        }
    private val mockedOppfolgingsSak: SakDTO
        get() {
            return SakDTO(UUID.randomUUID(), 123456, "ARBEIDSOPPFOLGING", "OPP")
        }
}



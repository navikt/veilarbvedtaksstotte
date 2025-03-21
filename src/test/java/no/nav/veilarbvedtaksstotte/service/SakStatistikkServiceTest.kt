package no.nav.veilarbvedtaksstotte.service

import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.utils.fn.UnsafeRunnable
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.client.person.dto.Adressebeskyttelse
import no.nav.veilarbvedtaksstotte.client.person.dto.Gradering
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingPeriodeDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.SakDTO
import no.nav.veilarbvedtaksstotte.config.EnvironmentProperties
import no.nav.veilarbvedtaksstotte.controller.dto.OppdaterUtkastDTO
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst
import no.nav.veilarbvedtaksstotte.domain.VedtakOpplysningKilder
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingMetode
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingStatus
import no.nav.veilarbvedtaksstotte.domain.vedtak.*
import no.nav.veilarbvedtaksstotte.repository.*
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils
import no.nav.veilarbvedtaksstotte.utils.TestData
import org.junit.jupiter.api.*
import org.mockito.kotlin.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class SakStatistikkServiceTest : DatabaseTest() {

    companion object {
        private var vedtaksstotteRepository: VedtaksstotteRepository? = null
        private var sakStatistikkService: SakStatistikkService? = null
        private var sakStatistikkRepository: SakStatistikkRepository? = null
        private var vedtakService: VedtakService? = null
        private var authService: AuthService = mock()
        private var kilderRepository: KilderRepository = mock()
        private val aktorOppslagClient: AktorOppslagClient = mock()
        private val veilarboppfolgingClient: VeilarboppfolgingClient = mock()
        private val veilarbpersonClient: VeilarbpersonClient = mock()
        private val environmentProperties: EnvironmentProperties = mock()

        @JvmStatic
        @BeforeAll
        fun setupOnce() {
            vedtaksstotteRepository = VedtaksstotteRepository(jdbcTemplate, transactor)
            sakStatistikkRepository = SakStatistikkRepository(jdbcTemplate)

            sakStatistikkService = SakStatistikkService(
                sakStatistikkRepository!!,
                veilarboppfolgingClient,
                aktorOppslagClient,
                mock(),
                environmentProperties,
                veilarbpersonClient
            )
            vedtakService = VedtakService(
                transactor,
                vedtaksstotteRepository,
                mock(),
                kilderRepository,
                mock(),
                mock(),
                authService,
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                sakStatistikkService
            )

        }
    }

    @BeforeEach
    fun setup() {
        DbTestUtils.cleanupDb(jdbcTemplate)
        doReturn(TestData.TEST_VEILEDER_IDENT).`when`(authService)?.innloggetVeilederIdent
        whenever(kilderRepository.hentKilderForVedtak(any())).thenReturn(listOf(Kilde()))
        whenever(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TestData.TEST_FNR)).thenReturn(
            AuthKontekst(TestData.TEST_FNR.get(), TestData.TEST_AKTOR_ID, TestData.TEST_OPPFOLGINGSENHET_ID)
        )
        whenever(
            authService.sjekkTilgangTilBrukerOgEnhet(
                TilgangType.SKRIVE,
                AktorId.of(TestData.TEST_AKTOR_ID)
            )
        ).thenReturn(AuthKontekst(TestData.TEST_FNR.get(), TestData.TEST_AKTOR_ID, TestData.TEST_OPPFOLGINGSENHET_ID))
        whenever(authService.getFnrOrThrow(TestData.TEST_AKTOR_ID)).thenReturn(TestData.TEST_FNR)

        whenever(aktorOppslagClient.hentFnr(AktorId.of(TestData.TEST_AKTOR_ID))).thenReturn(TestData.TEST_FNR)
        whenever(veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(TestData.TEST_FNR)).thenReturn(
            mockedOppfolgingsPeriode
        )
        whenever(veilarboppfolgingClient.hentOppfolgingsperiodeSak(any())).thenReturn(mockedOppfolgingsSak)
        whenever(environmentProperties.naisAppImage).thenReturn("naisAppImage")
        whenever(veilarbpersonClient.hentAdressebeskyttelse(any())).thenReturn(Adressebeskyttelse(Gradering.UGRADERT))
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
                lagretRad.fagsystemNavn, "Fagsystemnavn skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.fagsystemVersjon, "Fagsystemversjon skal være utfylt"
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
                lagretRad.fagsystemNavn, "Fagsystemnavn skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.fagsystemVersjon, "Fagsystemversjon skal være utfylt"
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
                lagretRad.fagsystemNavn, "Fagsystemnavn skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.fagsystemVersjon, "Fagsystemversjon skal være utfylt"
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
            val nestSisteRad = sakStatistikkRepository!!.hentSakStatistikkListe(TestData.TEST_AKTOR_ID).last()
            vedtakService!!.lagUtkast(TestData.TEST_FNR)
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
                lagretRad.fagsystemNavn, "Fagsystemnavn skal være utfylt"
            )
            Assertions.assertNotNull(
                lagretRad.fagsystemVersjon, "Fagsystemversjon skal være utfylt"
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

    private fun withContext(runnable: UnsafeRunnable) {
        AuthContextHolderThreadLocal.instance()
            .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, TestData.TEST_VEILEDER_IDENT), runnable)
    }

    private fun gittUtkastKlarForUtsendelse() {
        withContext {
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

            kilderRepository.lagKilder(kilder, utkast.id)
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
            val utkast = vedtaksstotteRepository!!.hentUtkast(TestData.TEST_AKTOR_ID)
            vedtakService!!.fattVedtak(utkast.id)
        }
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



package no.nav.veilarbvedtaksstotte.service;

import io.getunleash.DefaultUnleash;
import no.nav.common.abac.AbacClient;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.Credentials;
import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.poao_tilgang.client.Decision;
import no.nav.poao_tilgang.client.PoaoTilgangClient;
import no.nav.poao_tilgang.client.api.ApiResult;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.*;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbArenaOppfolging;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettetJournalpostDTO;
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetKontaktinformasjon;
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetStedsadresse;
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClient;
import no.nav.veilarbvedtaksstotte.client.person.PersonNavn;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO.Adresse;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.Veileder;
import no.nav.veilarbvedtaksstotte.controller.dto.OppdaterUtkastDTO;
import no.nav.veilarbvedtaksstotte.domain.Målform;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus;
import no.nav.veilarbvedtaksstotte.repository.*;
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import org.joda.time.Instant;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO.AdresseType.NORSKPOSTADRESSE;
import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class VedtakServiceTest extends DatabaseTest {

    private static VedtaksstotteRepository vedtaksstotteRepository;
    private static KilderRepository kilderRepository;
    private static MeldingRepository meldingRepository;

    private static VedtakService vedtakService;
    private static OyeblikksbildeService oyeblikksbildeService;
    private static AuthService authService;

    private static final DefaultUnleash unleashService = mock(DefaultUnleash.class);
    private static final VedtakHendelserService vedtakHendelserService = mock(VedtakHendelserService.class);
    private static final VeilederService veilederService = mock(VeilederService.class);

    private static final VeilarbpersonClient veilarbpersonClient = mock(VeilarbpersonClient.class);
    private static final VeilarbregistreringClient registreringClient = mock(VeilarbregistreringClient.class);
    private static final AiaBackendClient AIA_BACKEND_CLIENT = mock(AiaBackendClient.class);
    private static final RegoppslagClient regoppslagClient = mock(RegoppslagClient.class);
    private static final AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);
    private static final VeilarbarenaClient veilarbarenaClient = mock(VeilarbarenaClient.class);
    private static final AbacClient abacClient = mock(AbacClient.class);
    private static final DokarkivClient dokarkivClient = mock(DokarkivClient.class);
    private static final PdfClient pdfClient = mock(PdfClient.class);
    private static final VeilarbveilederClient veilarbveilederClient = mock(VeilarbveilederClient.class);
    private static final UtrullingService utrullingService = mock(UtrullingService.class);
    private static final EnhetInfoService enhetInfoService = mock(EnhetInfoService.class);
    private static final MetricsService metricsService = mock(MetricsService.class);

    private static final VeilarbPep veilarbPep = mock(VeilarbPep.class);
    private static final Credentials credentials = mock(Credentials.class);
    private static final PoaoTilgangClient poaoTilgangClient = mock(PoaoTilgangClient.class);

    private static final String CV_DATA = "{\"cv\": \"cv\"}";
    private static final String EGENVURDERING_DATO = new Instant().toString();

    @BeforeAll
    public static void setupOnce() {
        VeilarbarenaService veilarbarenaService = new VeilarbarenaService(veilarbarenaClient);
        kilderRepository = spy(new KilderRepository(jdbcTemplate));
        meldingRepository = spy(new MeldingRepository(jdbcTemplate));
        vedtaksstotteRepository = new VedtaksstotteRepository(jdbcTemplate, transactor);
        OyeblikksbildeRepository oyeblikksbildeRepository = new OyeblikksbildeRepository(jdbcTemplate);
        BeslutteroversiktRepository beslutteroversiktRepository = new BeslutteroversiktRepository(jdbcTemplate);

        authService = spy(new AuthService(aktorOppslagClient, veilarbPep, veilarbarenaService, abacClient, credentials, AuthContextHolderThreadLocal.instance(), utrullingService, poaoTilgangClient, unleashService));
        oyeblikksbildeService = new OyeblikksbildeService(authService, oyeblikksbildeRepository, vedtaksstotteRepository, veilarbpersonClient, registreringClient, AIA_BACKEND_CLIENT);
        MalTypeService malTypeService = new MalTypeService(registreringClient);
        DokumentService dokumentService = new DokumentService(
                regoppslagClient,
                pdfClient,
                veilarbarenaClient,
                veilarbpersonClient,
                veilarbveilederClient,
                dokarkivClient,
                enhetInfoService,
                malTypeService);
        vedtakService = new VedtakService(
                transactor,
                vedtaksstotteRepository,
                beslutteroversiktRepository,
                kilderRepository,
                meldingRepository,
                null,
                authService,
                oyeblikksbildeService,
                veilederService,
                vedtakHendelserService,
                dokumentService,
                veilarbarenaService,
                metricsService);
    }

    @BeforeEach
    public void setup() {
        Map<String,String> egenvurderingstekster = new HashMap<>();
        egenvurderingstekster.put("STANDARD_INNSATS", "Svar jeg klarer meg");
        EgenvurderingResponseDTO egenvurderingResponse = new EgenvurderingResponseDTO(EGENVURDERING_DATO, "123456", "STANDARD_INNSATS", new EgenvurderingResponseDTO.Tekster("testspm", egenvurderingstekster));
        DbTestUtils.cleanupDb(jdbcTemplate);
        reset(veilederService);
        reset(meldingRepository);
        reset(unleashService);
        reset(dokarkivClient);
        reset(vedtakHendelserService);
        doReturn(TEST_VEILEDER_IDENT).when(authService).getInnloggetVeilederIdent();
        doReturn(UUID.randomUUID()).when(authService).hentInnloggetVeilederUUID();
        when(veilederService.hentEnhetNavn(TEST_OPPFOLGINGSENHET_ID)).thenReturn(TEST_OPPFOLGINGSENHET_NAVN);
        when(veilederService.hentVeileder(TEST_VEILEDER_IDENT)).thenReturn(new Veileder(TEST_VEILEDER_IDENT, TEST_VEILEDER_NAVN));
        when(veilederService.hentVeilederEllerNull(TEST_VEILEDER_IDENT)).thenReturn(Optional.of(new Veileder(TEST_VEILEDER_IDENT, TEST_VEILEDER_NAVN)));
        when(regoppslagClient.hentPostadresse(any())).thenReturn(
                new RegoppslagResponseDTO("", new Adresse(NORSKPOSTADRESSE, "", "", "", "", "", "", "")));
        when(veilarbpersonClient.hentCVOgJobbprofil(TEST_FNR.get())).thenReturn(CV_DATA);
        when(veilarbpersonClient.hentMålform(TEST_FNR)).thenReturn(Målform.NB);
        when(veilarbpersonClient.hentPersonNavn(TEST_FNR.get())).thenReturn(new PersonNavn("Fornavn", null, "Etternavn", null));
        when(registreringClient.hentRegistreringDataJson(TEST_FNR.get())).thenReturn(getRegistreringsdata());
        when(AIA_BACKEND_CLIENT.hentEgenvurdering(new EgenvurderingForPersonDTO(TEST_FNR.get()))).thenReturn(egenvurderingResponse);
        when(AIA_BACKEND_CLIENT.hentEndringIRegistreringdata(new EndringIRegistreringdataRequest(TEST_FNR.get()))).thenReturn(getEndringIRegistreringsdataResponse());
        when(aktorOppslagClient.hentAktorId(TEST_FNR)).thenReturn(AktorId.of(TEST_AKTOR_ID));
        when(aktorOppslagClient.hentFnr(AktorId.of(TEST_AKTOR_ID))).thenReturn(TEST_FNR);
        when(veilarbarenaClient.hentOppfolgingsbruker(TEST_FNR)).thenReturn(Optional.of(new VeilarbArenaOppfolging(TEST_OPPFOLGINGSENHET_ID, "ARBS", "IKVAL")));
        when(veilarbarenaClient.oppfolgingssak(TEST_FNR)).thenReturn(Optional.of(TEST_OPPFOLGINGSSAK));
        when(dokarkivClient.opprettJournalpost(any()))
                .thenReturn(new OpprettetJournalpostDTO(
                        TEST_JOURNALPOST_ID,
                        true,
                        Arrays.asList(new OpprettetJournalpostDTO.DokumentInfoId(TEST_DOKUMENT_ID))));
        when(veilarbveilederClient.hentVeileder(TEST_VEILEDER_IDENT)).thenReturn(new Veileder(TEST_VEILEDER_IDENT, TEST_VEILEDER_NAVN));
        when(enhetInfoService.hentEnhet(EnhetId.of(TEST_OPPFOLGINGSENHET_ID))).thenReturn(new Enhet().setNavn(TEST_OPPFOLGINGSENHET_NAVN));
        when(enhetInfoService.utledEnhetKontaktinformasjon(EnhetId.of(TEST_OPPFOLGINGSENHET_ID)))
                .thenReturn(new EnhetKontaktinformasjon(EnhetId.of(TEST_OPPFOLGINGSENHET_ID), new EnhetStedsadresse("","","","","",""), ""));
        when(pdfClient.genererPdf(any())).thenReturn(new byte[]{});
        when(poaoTilgangClient.evaluatePolicy(any())).thenReturn(new ApiResult<>(null, Decision.Permit.INSTANCE));
    }

    @Test
    public void fattVedtak__skal_feile_hvis_iserv() {
        when(veilarbarenaClient.hentOppfolgingsbruker(TEST_FNR)).thenReturn(Optional.of(new VeilarbArenaOppfolging(TEST_OPPFOLGINGSENHET_ID, "ISERV", "IVURD")));
        gittUtkastKlarForUtsendelse();

        assertThrows(IllegalStateException.class, () ->  fattVedtak());
    }


    @Test
    public void fattVedtak__opprett_oppdater_og_journalforer_og_ferdigstiller_vedtak() {

        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);
            assertNyttUtkast();
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            OppdaterUtkastDTO oppdaterDto = new OppdaterUtkastDTO()
                    .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                    .setBegrunnelse("En begrunnelse")
                    .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                    .setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

            vedtakService.oppdaterUtkast(utkast.getId(), oppdaterDto);
            assertOppdatertUtkast(oppdaterDto);

            vedtakService.fattVedtak(utkast.getId());
            assertJournalførtOgFerdigstilltVedtak();
        });
    }

    @Test
    public void fattVedtak__opprett_oppdater_og_send_vedtak() {
        gittUtkastKlarForUtsendelse();

        fattVedtak();

        assertJournalførtOgFerdigstilltVedtak();
    }

    @Test
    public void lagUtkast__skal_opprette_system_melding() {
        withContext(() -> {
            gittTilgang();
            vedtakService.lagUtkast(TEST_FNR);

            verify(meldingRepository, times(1))
                    .opprettSystemMelding(anyLong(), eq(SystemMeldingType.UTKAST_OPPRETTET), eq(TEST_VEILEDER_IDENT));
        });
    }

    @Test
    public void oppdaterUtkast__skal_ikke_endre_kilder_hvis_ikke_endret() {
        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            List<String> kilder = List.of("test1", "test2");
            kilderRepository.lagKilder(kilder, utkast.getId());

            OppdaterUtkastDTO oppdaterUtkastDTO = new OppdaterUtkastDTO();
            oppdaterUtkastDTO.setOpplysninger(kilder);

            vedtakService.oppdaterUtkast(utkast.getId(), oppdaterUtkastDTO);

            verify(kilderRepository, never())
                    .slettKilder(utkast.getId());

            verify(kilderRepository, times(1))
                    .lagKilder(kilder, utkast.getId());
        });
    }

    @Test
    public void oppdaterUtkast__skal_endre_kilder_hvis_endret() {
        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            List<String> gamleKilder = List.of("test1", "test2");
            List<String> nyeKilder = List.of("test1", "test3");
            kilderRepository.lagKilder(gamleKilder, utkast.getId());

            OppdaterUtkastDTO oppdaterUtkastDTO = new OppdaterUtkastDTO();
            oppdaterUtkastDTO.setOpplysninger(nyeKilder);

            vedtakService.oppdaterUtkast(utkast.getId(), oppdaterUtkastDTO);

            verify(kilderRepository, times(1))
                    .slettKilder(utkast.getId());

            verify(kilderRepository, times(1))
                    .lagKilder(nyeKilder, utkast.getId());
        });
    }

    @Test
    public void oppdaterUtkast__feiler_for_veileder_som_ikke_er_satt_pa_utkast() {
        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT + "annen");

            assertThatThrownBy(() -> {
                        vedtakService.fattVedtak(utkast.getId());
                        vedtakService.oppdaterUtkast(utkast.getId(), new OppdaterUtkastDTO());
                    }
            ).isExactlyInstanceOf(ResponseStatusException.class);
        });
    }

    @Test
    public void slettUtkast__skal_slette_utkast_med_data() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        kilderRepository.lagKilder(TEST_KILDER, utkast.getId());

        meldingRepository.opprettDialogMelding(utkast.getId(), null, "Test");

        vedtakService.slettUtkast(utkast);

        assertNull(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID));
    }

    @Test
    public void slettUtkast__feiler_for_veileder_som_ikke_er_satt_pa_utkast() {
        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);

            when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT + "annen");

            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            assertThatThrownBy(() ->
                    vedtakService.slettUtkastSomVeileder(utkast.getId())
            ).isExactlyInstanceOf(ResponseStatusException.class);
        });
    }

    @Test
    public void fattVedtak__journalforer_og_ferdigstiller_vedtak() {
        gittUtkastKlarForUtsendelse();

        when(dokarkivClient.opprettJournalpost(any()))
                .thenReturn(new OpprettetJournalpostDTO(
                        TEST_JOURNALPOST_ID,
                        false,
                        Arrays.asList(new OpprettetJournalpostDTO.DokumentInfoId(TEST_DOKUMENT_ID))));

        fattVedtak();

        assertJournalførtOgFerdigstilltVedtak();
    }

    @Test
    public void fattVedtak__feiler_for_veileder_som_ikke_er_satt_pa_utkast() {
        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);

            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            vedtakService.oppdaterUtkast(utkast.getId(),
                    new OppdaterUtkastDTO()
                            .setBegrunnelse("begrunnelse")
                            .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                            .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                            .setOpplysninger(Collections.singletonList("opplysning")));

            when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT + "annen");

            assertThatThrownBy(() ->
                    vedtakService.fattVedtak(utkast.getId())
            ).isExactlyInstanceOf(ResponseStatusException.class);
        });
    }

    @Test
    public void taOverUtkast__setter_ny_veileder() {
        withContext(() -> {
            String tidligereVeilederId = TEST_VEILEDER_IDENT + "tidligere";
            gittTilgang();
            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, tidligereVeilederId, TEST_OPPFOLGINGSENHET_ID);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            assertEquals(tidligereVeilederId, vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getVeilederIdent());
            vedtakService.taOverUtkast(utkast.getId());
            assertEquals(TEST_VEILEDER_IDENT, vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getVeilederIdent());
        });
    }

    @Test
    public void taOverUtkast__fjerner_beslutter_hvis_veileder_er_beslutter() {
        withContext(() -> {
            gittTilgang();
            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT + "tidligere", TEST_OPPFOLGINGSENHET_ID);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);
            vedtaksstotteRepository.setBeslutter(utkast.getId(), TEST_VEILEDER_IDENT);

            vedtakService.taOverUtkast(utkast.getId());
            assertNull(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getBeslutterIdent());
        });
    }

    @Test
    public void taOverUtkast__oppretter_system_melding() {
        withContext(() -> {
            String tidligereVeilederId = TEST_VEILEDER_IDENT + "tidligere";
            gittTilgang();

            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, tidligereVeilederId, TEST_OPPFOLGINGSENHET_ID);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            reset(meldingRepository);

            vedtakService.taOverUtkast(utkast.getId());

            verify(meldingRepository, times(1))
                    .opprettSystemMelding(anyLong(), eq(SystemMeldingType.TATT_OVER_SOM_VEILEDER), eq(TEST_VEILEDER_IDENT));
        });
    }

    @Test
    public void taOverUtkast__feiler_dersom_ikke_utkast() {
        withContext(() -> {
            assertThatThrownBy(() ->
                    vedtakService.taOverUtkast(123)
            ).hasMessage("404 NOT_FOUND \"Fant ikke utkast\"");
        });
    }

    @Test
    public void taOverUtkast__feiler_dersom_ingen_tilgang() {
        assertThatThrownBy(() ->
                vedtakService.taOverUtkast(123)
        ).isExactlyInstanceOf(ResponseStatusException.class);
    }

    @Test
    public void taOverUtkast__feiler_dersom_samme_veileder() {
        withContext(() -> {
            gittTilgang();
            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            assertThatThrownBy(() ->
                    vedtakService.taOverUtkast(utkast.getId())
            ).isExactlyInstanceOf(ResponseStatusException.class);
        });
    }

    private void gittTilgang() {
        when(utrullingService.erUtrullet(any())).thenReturn(true);
        when(veilarbPep.harVeilederTilgangTilPerson(any(NavIdent.class), any(), any())).thenReturn(true);
        when(veilarbPep.harVeilederTilgangTilEnhet(any(NavIdent.class), any())).thenReturn(true);
    }

    private void withContext(UnsafeRunnable runnable) {
        AuthContextHolderThreadLocal
                .instance()
                .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, TEST_VEILEDER_IDENT), runnable);
    }

    private void gittUtkastKlarForUtsendelse() {
        withContext(() -> {
            gittTilgang();
            vedtakService.lagUtkast(TEST_FNR);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            OppdaterUtkastDTO oppdaterDto = new OppdaterUtkastDTO()
                    .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                    .setBegrunnelse("En begrunnelse")
                    .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                    .setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

            List<String> kilder = List.of("opp1", "opp2");

            kilderRepository.lagKilder(kilder, utkast.getId());

            vedtakService.oppdaterUtkast(utkast.getId(), oppdaterDto);

        });
    }

    private void assertNyttUtkast() {
        Vedtak opprettetUtkast = vedtakService.hentUtkast(TEST_FNR);
        assertEquals(VedtakStatus.UTKAST, opprettetUtkast.getVedtakStatus());
        assertEquals(TEST_VEILEDER_IDENT, opprettetUtkast.getVeilederIdent());
        assertEquals(TEST_VEILEDER_NAVN, opprettetUtkast.getVeilederNavn());
        assertEquals(TEST_OPPFOLGINGSENHET_ID, opprettetUtkast.getOppfolgingsenhetId());
        assertEquals(TEST_OPPFOLGINGSENHET_NAVN, opprettetUtkast.getOppfolgingsenhetNavn());
        assertFalse(opprettetUtkast.isGjeldende());
        assertEquals(opprettetUtkast.getOpplysninger().size(), 0);
        assertFalse(opprettetUtkast.isSender());
    }

    private Vedtak hentVedtak() {
        List<Vedtak> vedtakList = vedtakService.hentFattedeVedtak(TEST_FNR);
        assertEquals(vedtakList.size(), 1);
        return vedtakList.get(0);
    }

    private void fattVedtak() {
        withContext(() -> {
            gittTilgang();
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);
            vedtakService.fattVedtak(utkast.getId());
        });
    }

    private void assertOppdatertUtkast(OppdaterUtkastDTO dto) {
        Vedtak oppdatertUtkast = vedtakService.hentUtkast(TEST_FNR);
        assertEquals(dto.getHovedmal(), oppdatertUtkast.getHovedmal());
        assertEquals(dto.getBegrunnelse(), oppdatertUtkast.getBegrunnelse());
        assertEquals(dto.getInnsatsgruppe(), oppdatertUtkast.getInnsatsgruppe());
        assertThat(oppdatertUtkast.getOpplysninger(), containsInAnyOrder(dto.getOpplysninger().toArray(new String[0])));
    }

    private void assertJournalførtOgFerdigstilltVedtak() {
        withContext(() -> {
            gittTilgang();
            Vedtak sendtVedtak = hentVedtak();
            assertTrue(sendtVedtak.isGjeldende());
            assertEquals(VedtakStatus.SENDT, sendtVedtak.getVedtakStatus());
            assertEquals(TEST_DOKUMENT_ID, sendtVedtak.getDokumentInfoId());
            assertEquals(TEST_JOURNALPOST_ID, sendtVedtak.getJournalpostId());
            assertOyeblikksbildeForFattetVedtak(sendtVedtak.getId());
        });
        verify(vedtakHendelserService).vedtakSendt(any());
    }

    private void assertOyeblikksbildeForFattetVedtak(long vedtakId) {
        String egenvurderingdata = getEgenvurderingData();
        withContext(() -> {
            List<Oyeblikksbilde> oyeblikksbilde = oyeblikksbildeService.hentOyeblikksbildeForVedtak(vedtakId);
            assertThat(oyeblikksbilde, containsInAnyOrder(
                    equalTo(new Oyeblikksbilde(vedtakId, OyeblikksbildeType.REGISTRERINGSINFO, getOppdatertRegistreringsdata())),
                    equalTo(new Oyeblikksbilde(vedtakId, OyeblikksbildeType.CV_OG_JOBBPROFIL, CV_DATA)),
                    equalTo(new Oyeblikksbilde(vedtakId, OyeblikksbildeType.EGENVURDERING, egenvurderingdata)))
            );
        });
    }

    private String getEgenvurderingData() {
        return "{\"sistOppdatert\":\""+EGENVURDERING_DATO+"\",\"svar\":[{\"spm\":\"testspm\",\"svar\":\"Svar jeg klarer meg\",\"oppfolging\":\"STANDARD_INNSATS\",\"dialogId\":\"123456\"}]}";
    }

    private String getRegistreringsdata() {
        return "{\"registrering\":{\"id\":10004240,\"opprettetDato\":\"2023-06-22T16:47:18.325956+02:00\",\"besvarelse\":{\"utdanning\":\"HOYERE_UTDANNING_5_ELLER_MER\",\"utdanningBestatt\":\"JA\",\"utdanningGodkjent\":\"JA\",\"helseHinder\":\"NEI\",\"andreForhold\":\"NEI\"," +
                "\"sisteStilling\":\"INGEN_SVAR\",\"dinSituasjon\":\"MISTET_JOBBEN\",\"fremtidigSituasjon\":null,\"tilbakeIArbeid\":null},\"teksterForBesvarelse\":[{\"sporsmalId\":\"dinSituasjon\",\"sporsmal\":\"Velg den situasjonen som passer deg best\",\"svar\":\"Har mistet eller kommer til å miste jobben\"},{\"sporsmalId\":\"utdanning\",\"sporsmal\":\"Hva er din høyeste fullførte utdanning?\"," +
                "\"svar\":\"Høyere utdanning (5 år eller mer)\"},{" +
                "\"sporsmalId\":\"utdanningGodkjent\",\"sporsmal\":\"Er utdanningen din godkjent i Norge?\",\"svar\":\"Ja\"},{\"sporsmalId\":\"utdanningBestatt\",\"sporsmal\":\"Er utdanningen din bestått?\",\"svar\":\"Ja\"},{\"sporsmalId\":\"andreForhold\",\"sporsmal\":\"Har du andre problemer med å søke eller være i jobb?\",\"svar\":\"Nei\"},{" +
                "\"sporsmalId\":\"sisteStilling\",\"sporsmal\":\"Hva er din siste jobb?\",\"svar\":\"Annen stilling\"},{" +
                "\"sporsmalId\":\"helseHinder\",\"sporsmal\":\"Har du helseproblemer som hindrer deg i å søke eller være i jobb?\",\"svar\":\"Nei\"}]," +
                "\"sisteStilling\": {\"label\":\"Annen stilling\",\"konseptId\": -1,\"styrk08\":\"-1\"}," +
                "\"profilering\": {\"innsatsgruppe\":\"SITUASJONSBESTEMT_INNSATS\",\"alder\": 28,\"jobbetSammenhengendeSeksAvTolvSisteManeder\": false}," +
                "\"manueltRegistrertAv\": null},\"type\":\"ORDINAER\"}";
    }

    private String getOppdatertRegistreringsdata() {
        return new JSONObject(
        "{\"registrering\":{\"id\":10004240,\"opprettetDato\":\"2023-06-22T16:47:18.325956+02:00\",\"besvarelse\":{\"utdanning\":\"HOYERE_UTDANNING_5_ELLER_MER\",\"utdanningBestatt\":\"JA\",\"utdanningGodkjent\":\"JA\",\"helseHinder\":\"NEI\",\"andreForhold\":\"NEI\"," +
                "\"sisteStilling\":\"INGEN_SVAR\",\"dinSituasjon\":\"OPPSIGELSE\",\"fremtidigSituasjon\":null,\"tilbakeIArbeid\":null},\"teksterForBesvarelse\":[{\"sporsmalId\":\"dinSituasjon\",\"sporsmal\":\"Velg den situasjonen som passer deg best\",\"svar\":\"Jeg har blitt sagt opp av arbeidsgiver\"},{\"sporsmalId\":\"utdanning\",\"sporsmal\":\"Hva er din høyeste fullførte utdanning?\"," +
                "\"svar\":\"Høyere utdanning (5 år eller mer)\"},{" +
                "\"sporsmalId\":\"utdanningGodkjent\",\"sporsmal\":\"Er utdanningen din godkjent i Norge?\",\"svar\":\"Ja\"},{\"sporsmalId\":\"utdanningBestatt\",\"sporsmal\":\"Er utdanningen din bestått?\",\"svar\":\"Ja\"},{\"sporsmalId\":\"andreForhold\",\"sporsmal\":\"Har du andre problemer med å søke eller være i jobb?\",\"svar\":\"Nei\"},{" +
                "\"sporsmalId\":\"sisteStilling\",\"sporsmal\":\"Hva er din siste jobb?\",\"svar\":\"Annen stilling\"},{" +
                "\"sporsmalId\":\"helseHinder\",\"sporsmal\":\"Har du helseproblemer som hindrer deg i å søke eller være i jobb?\",\"svar\":\"Nei\"}]," +
                "\"sisteStilling\": {\"label\":\"Annen stilling\",\"konseptId\": -1,\"styrk08\":\"-1\"}," +
                "\"profilering\": {\"innsatsgruppe\":\"SITUASJONSBESTEMT_INNSATS\",\"alder\": 28,\"jobbetSammenhengendeSeksAvTolvSisteManeder\": false}," +
                "\"manueltRegistrertAv\": null, \"endretAv\":\"BRUKER\", \"endretTidspunkt\":\"2023-07-18T11:24:03.158629\"},\"type\":\"ORDINAER\"}").toString();
    }

    private EndringIRegistreringsdataResponse getEndringIRegistreringsdataResponse() {
        return new EndringIRegistreringsdataResponse(
                10004400,
                new EndringIRegistreringsdataResponse.Besvarelse(
                        new EndringIRegistreringsdataResponse.Besvarelse.Utdanning("HOYERE_UTDANNING_1_TIL_4", null, null, null, null),
                        new EndringIRegistreringsdataResponse.Besvarelse.UtdanningBestatt("JA", null, null, null, null),
                        new EndringIRegistreringsdataResponse.Besvarelse.UtdanningGodkjent("JA", null, null, null, null),
                        new EndringIRegistreringsdataResponse.Besvarelse.HelseHinder("JA", null, null, null, null),
                        new EndringIRegistreringsdataResponse.Besvarelse.AndreForhold("NEI", null, null, null, null),
                        new EndringIRegistreringsdataResponse.Besvarelse.SisteStilling("INGEN_SVAR", null, null, null, null),
                        new EndringIRegistreringsdataResponse.Besvarelse.DinSituasjon(
                                "OPPSIGELSE",
                                new EndringIRegistreringsdataResponse.Besvarelse.DinSituasjon.TilleggsData(null, "2023-07-31", "2023-07-19", null, null, null, null, null),
                                null,
                                null,
                                "BRUKER",
                                "2023-07-18T11:24:03.136693338"
                        ),
                        new EndringIRegistreringsdataResponse.Besvarelse.FremtidigSituasjon(null, null, null, null, null),
                        new EndringIRegistreringsdataResponse.Besvarelse.TilbakeIArbeid(null, null, null, null, null)

                ),
                "BRUKER",
                "2023-07-18T11:24:03.158629",
                "2023-07-17T11:27:25.299658",
                "BRUKER",
                true
        );
    }
}

package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.abac.AbacClient;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.common.utils.fn.UnsafeSupplier;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbArenaOppfolging;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettetJournalpostDTO;
import no.nav.veilarbvedtaksstotte.client.dokument.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.client.dokument.VeilarbdokumentClient;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.VeilarbvedtakinfoClient;
import no.nav.veilarbvedtaksstotte.client.person.PersonNavn;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO.Adresse;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.Veileder;
import no.nav.veilarbvedtaksstotte.controller.dto.OppdaterUtkastDTO;
import no.nav.veilarbvedtaksstotte.domain.DistribusjonBestillingId;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO.AdresseType.NORSKPOSTADRESSE;
import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class VedtakServiceTest extends DatabaseTest {

    private static VedtaksstotteRepository vedtaksstotteRepository;
    private static KilderRepository kilderRepository;
    private static MeldingRepository meldingRepository;

    private static VedtakService vedtakService;
    private static OyeblikksbildeService oyeblikksbildeService;
    private static AuthService authService;

    private static final MetricsService metricsService = mock(MetricsService.class);
    private static final UnleashService unleashService = mock(UnleashService.class);
    private static final VedtakStatusEndringService vedtakStatusEndringService = mock(VedtakStatusEndringService.class);
    private static final VeilederService veilederService = mock(VeilederService.class);

    private static final VeilarbpersonClient veilarbpersonClient = mock(VeilarbpersonClient.class);
    private static final VeilarbregistreringClient registreringClient = mock(VeilarbregistreringClient.class);
    private static final VeilarbvedtakinfoClient egenvurderingClient = mock(VeilarbvedtakinfoClient.class);
    private static final RegoppslagClient regoppslagClient = mock(RegoppslagClient.class);
    private static final VeilarbdokumentClient veilarbdokumentClient = mock(VeilarbdokumentClient.class);
    private static final AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);
    private static final VeilarbarenaClient veilarbarenaClient = mock(VeilarbarenaClient.class);
    private static final AbacClient abacClient = mock(AbacClient.class);
    private static final DokarkivClient dokarkivClient = mock(DokarkivClient.class);
    private static final UtrullingService utrullingService = mock(UtrullingService.class);

    private static final VeilarbPep veilarbPep = mock(VeilarbPep.class);

    private static final String CV_DATA = "{\"cv\": \"cv\"}";
    private static final String REGISTRERING_DATA = "{\"registrering\": \"registrering\"}";
    private static final String EGENVURDERING_DATA = "{\"egenvurdering\": \"egenvurdering\"}";

    @BeforeClass
    public static void setupOnce() {
        VeilarbarenaService veilarbarenaService = new VeilarbarenaService(veilarbarenaClient);
        kilderRepository = spy(new KilderRepository(jdbcTemplate));
        meldingRepository = spy(new MeldingRepository(jdbcTemplate));
        vedtaksstotteRepository = new VedtaksstotteRepository(jdbcTemplate, transactor);
        OyeblikksbildeRepository oyeblikksbildeRepository = new OyeblikksbildeRepository(jdbcTemplate);
        BeslutteroversiktRepository beslutteroversiktRepository = new BeslutteroversiktRepository(jdbcTemplate);

        authService = spy(new AuthService(aktorOppslagClient, veilarbPep, veilarbarenaService, abacClient, null, AuthContextHolderThreadLocal.instance(), utrullingService));
        oyeblikksbildeService = new OyeblikksbildeService(authService, oyeblikksbildeRepository, vedtaksstotteRepository, veilarbpersonClient, registreringClient, egenvurderingClient);
        MalTypeService malTypeService = new MalTypeService(registreringClient);
        DokumentServiceV2 dokumentServiceV2 = new DokumentServiceV2(regoppslagClient, veilarbdokumentClient, veilarbarenaClient, dokarkivClient);
        vedtakService = new VedtakService(
                transactor,
                vedtaksstotteRepository,
                beslutteroversiktRepository,
                kilderRepository,
                meldingRepository,
                veilarbdokumentClient,
                null,
                authService,
                unleashService,
                metricsService,
                oyeblikksbildeService,
                veilederService,
                malTypeService,
                vedtakStatusEndringService,
                dokumentServiceV2,
                veilarbarenaService);
    }

    @Before
    public void setup() {
        DbTestUtils.cleanupDb(jdbcTemplate);
        reset(veilederService);
        reset(veilarbdokumentClient);
        reset(meldingRepository);
        reset(unleashService);
        reset(dokarkivClient);
        reset(vedtakStatusEndringService);
        doReturn(TEST_VEILEDER_IDENT).when(authService).getInnloggetVeilederIdent();
        when(veilederService.hentEnhetNavn(TEST_OPPFOLGINGSENHET_ID)).thenReturn(TEST_OPPFOLGINGSENHET_NAVN);
        when(veilederService.hentVeileder(TEST_VEILEDER_IDENT)).thenReturn(new Veileder().setIdent(TEST_VEILEDER_IDENT).setNavn(TEST_VEILEDER_NAVN));
        when(veilederService.hentVeilederEllerNull(TEST_VEILEDER_IDENT)).thenReturn(Optional.of(new Veileder().setIdent(TEST_VEILEDER_IDENT).setNavn(TEST_VEILEDER_NAVN)));
        when(veilarbdokumentClient.sendDokument(any())).thenReturn(new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID));
        when(veilarbdokumentClient.produserDokumentV2(any())).thenReturn("dokument".getBytes());
        when(regoppslagClient.hentPostadresse(any())).thenReturn(
                new RegoppslagResponseDTO("", new Adresse(NORSKPOSTADRESSE, "", "", "", "", "", "", "")));
        when(veilarbpersonClient.hentCVOgJobbprofil(TEST_FNR.get())).thenReturn(CV_DATA);
        when(registreringClient.hentRegistreringDataJson(TEST_FNR.get())).thenReturn(REGISTRERING_DATA);
        when(egenvurderingClient.hentEgenvurdering(TEST_FNR.get())).thenReturn(EGENVURDERING_DATA);
        when(aktorOppslagClient.hentAktorId(TEST_FNR)).thenReturn(AktorId.of(TEST_AKTOR_ID));
        when(aktorOppslagClient.hentFnr(AktorId.of(TEST_AKTOR_ID))).thenReturn(TEST_FNR);
        when(veilarbarenaClient.hentOppfolgingsbruker(TEST_FNR)).thenReturn(new VeilarbArenaOppfolging(TEST_OPPFOLGINGSENHET_ID, "IKVAL"));
        when(veilarbarenaClient.oppfolgingssak(TEST_FNR)).thenReturn(TEST_OPPFOLGINGSSAK);
        when(veilarbpersonClient.hentPersonNavn(TEST_FNR.get())).thenReturn(new PersonNavn("Fornavn", null, "Etternavn", null));
        when(dokarkivClient.opprettJournalpost(any()))
                .thenReturn(new OpprettetJournalpostDTO(
                        TEST_JOURNALPOST_ID,
                        true,
                        Arrays.asList(new OpprettetJournalpostDTO.DokumentInfoId(TEST_DOKUMENT_ID))));
    }

    @Test(expected = IllegalStateException.class)
    public void fattVedtak__skal_feile_hvis_iserv() {
        when(veilarbarenaClient.hentOppfolgingsbruker(TEST_FNR)).thenReturn(new VeilarbArenaOppfolging(TEST_OPPFOLGINGSENHET_ID, "ISERV"));
        gittUtkastKlarForUtsendelse();
        fattVedtak();
    }


    @Test
    public void fattVedtak__opprett_oppdater_og_send_vedtak() {
        gittVersjon1AvFattVedtak();

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
            assertSendtVedtak();
        });
    }

    @Test
    public void fattVedtakV2__opprett_oppdater_og_send_vedtak() {
        gittVersjon2AvFattVedtak();
        gittUtkastKlarForUtsendelse();

        fattVedtak();

        assertJournalførtOgFerdigstilltVedtakV2();
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
    public void fattVedtakV2__journalforer_og_ferdigstiller_vedtak() {
        gittVersjon2AvFattVedtak();
        gittUtkastKlarForUtsendelse();

        when(dokarkivClient.opprettJournalpost(any()))
                .thenReturn(new OpprettetJournalpostDTO(
                        TEST_JOURNALPOST_ID,
                        false,
                        Arrays.asList(new OpprettetJournalpostDTO.DokumentInfoId(TEST_DOKUMENT_ID))));

        fattVedtak();

        assertJournalførtOgFerdigstilltVedtakV2();
    }

    private void fattVedtak() {
        withContext(() -> {
            gittTilgang();
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);
            vedtakService.fattVedtak(utkast.getId());
        });
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
    @Ignore // Testen er ustabil på GHA
    public void fattVedtak_sender_ikke_mer_enn_en_gang() {
        when(veilarbdokumentClient.sendDokument(any()))
                .thenAnswer(new AnswersWithDelay(10, // Simulerer tregt API
                        invocation -> new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID)));

        withContext(() -> {
            gittTilgang();
            gittUtkastKlarForUtsendelse();

            long id = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

            Stream<UnsafeSupplier<Future<?>>> stream = Stream.of(
                    (UnsafeSupplier<Future<?>>) () -> sendVedtakAsynk(id),
                    () -> sendVedtakAsynk(id),
                    () -> sendVedtakAsynk(id)
            ).parallel();

            stream.forEach(f -> {
                try {
                    f.get().get();
                } catch (Exception ignored) {
                }
            });

            verify(veilarbdokumentClient, times(1)).sendDokument(any());
        });
    }

    @Test
    public void fattVedtak__korrekt_sender_tilstand_dersom_send_dokument_feiler() {
        when(veilarbdokumentClient.sendDokument(any())).thenThrow(new RuntimeException());

        gittVersjon1AvFattVedtak();
        gittUtkastKlarForUtsendelse();

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        withContext(() -> {
            gittTilgang();
            assertThatThrownBy(() ->
                    vedtakService.fattVedtak(utkast.getId())).isExactlyInstanceOf(RuntimeException.class);

            assertFalse(vedtaksstotteRepository.hentUtkastEllerFeil(utkast.getId()).isSender());
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

    private void gittVersjon2AvFattVedtak() {
        when(unleashService.isNyDokIntegrasjonDisabled()).thenReturn(false);
    }

    private void gittVersjon1AvFattVedtak() {
        when(unleashService.isNyDokIntegrasjonDisabled()).thenReturn(true);
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

    private void assertOppdatertUtkast(OppdaterUtkastDTO dto) {
        Vedtak oppdatertUtkast = vedtakService.hentUtkast(TEST_FNR);
        assertEquals(dto.getHovedmal(), oppdatertUtkast.getHovedmal());
        assertEquals(dto.getBegrunnelse(), oppdatertUtkast.getBegrunnelse());
        assertEquals(dto.getInnsatsgruppe(), oppdatertUtkast.getInnsatsgruppe());
        assertThat(oppdatertUtkast.getOpplysninger(), containsInAnyOrder(dto.getOpplysninger().toArray(new String[0])));
    }

    private void assertJournalførtOgFerdigstilltVedtakV2() {
        withContext(() -> {
            gittTilgang();
            Vedtak sendtVedtak = hentVedtak();
            assertTrue(sendtVedtak.isGjeldende());
            assertEquals(VedtakStatus.SENDT, sendtVedtak.getVedtakStatus());
            assertEquals(TEST_DOKUMENT_ID, sendtVedtak.getDokumentInfoId());
            assertEquals(TEST_JOURNALPOST_ID, sendtVedtak.getJournalpostId());
            assertOyeblikksbildeForFattetVedtak(sendtVedtak.getId());
        });
        verify(vedtakStatusEndringService).vedtakSendt(any(), any());
    }

    private void assertSendtVedtak() {
        withContext(() -> {
            gittTilgang();
            Vedtak sendtVedtak = hentVedtak();
            assertEquals(VedtakStatus.SENDT, sendtVedtak.getVedtakStatus());
            assertEquals(TEST_DOKUMENT_ID, sendtVedtak.getDokumentInfoId());
            assertEquals(TEST_JOURNALPOST_ID, sendtVedtak.getJournalpostId());
            assertEquals(DistribusjonBestillingId.Mangler.INSTANCE.getId(), sendtVedtak.getDokumentbestillingId());
            assertTrue(sendtVedtak.isGjeldende());
            assertFalse(sendtVedtak.isSender());
            assertOyeblikksbildeForFattetVedtak(sendtVedtak.getId());
        });
    }

    private void assertOyeblikksbildeForFattetVedtak(long vedtakId) {
        withContext(() -> {
            List<Oyeblikksbilde> oyeblikksbilde = oyeblikksbildeService.hentOyeblikksbildeForVedtak(vedtakId);
            assertThat(oyeblikksbilde, containsInAnyOrder(
                    equalTo(new Oyeblikksbilde(vedtakId, OyeblikksbildeType.REGISTRERINGSINFO, REGISTRERING_DATA)),
                    equalTo(new Oyeblikksbilde(vedtakId, OyeblikksbildeType.CV_OG_JOBBPROFIL, CV_DATA)),
                    equalTo(new Oyeblikksbilde(vedtakId, OyeblikksbildeType.EGENVURDERING, EGENVURDERING_DATA)))
            );
        });
    }

    ExecutorService executorService = Executors.newFixedThreadPool(3);

    private Future<?> sendVedtakAsynk(long id) {
        return executorService.submit(() ->
                withContext(() ->
                        vedtakService.fattVedtak(id)));
    }
}

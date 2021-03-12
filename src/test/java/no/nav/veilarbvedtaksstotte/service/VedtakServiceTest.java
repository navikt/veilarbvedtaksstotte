package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.abac.AbacClient;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.common.utils.fn.UnsafeSupplier;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettetJournalpostDTO;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DistribuerJournalpostResponsDTO;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient;
import no.nav.veilarbvedtaksstotte.client.dokument.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.client.dokument.VeilarbdokumentClient;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.VeilarbvedtakinfoClient;
import no.nav.veilarbvedtaksstotte.client.person.PersonNavn;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.Veileder;
import no.nav.veilarbvedtaksstotte.controller.dto.OppdaterUtkastDTO;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.repository.*;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static no.nav.common.utils.EnvironmentUtils.NAIS_CLUSTER_NAME_PROPERTY_NAME;
import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class VedtakServiceTest {

    private static JdbcTemplate db;
    private static TransactionTemplate transactor;

    private static VedtaksstotteRepository vedtaksstotteRepository;
    private static KilderRepository kilderRepository;
    private static MeldingRepository meldingRepository;
    private static OyeblikksbildeRepository oyeblikksbildeRepository;
    private static BeslutteroversiktRepository beslutteroversiktRepository;

    private static VedtakService vedtakService;
    private static OyeblikksbildeService oyeblikksbildeService;
    private static MalTypeService malTypeService;
    private static AuthService authService;
    private static DokumentServiceV2 dokumentServiceV2;

    private static final MetricsService metricsService = mock(MetricsService.class);
    private static final UnleashService unleashService = mock(UnleashService.class);
    private static final VedtakStatusEndringService vedtakStatusEndringService = mock(VedtakStatusEndringService.class);
    private static final VeilederService veilederService = mock(VeilederService.class);

    private static final VeilarbpersonClient veilarbpersonClient = mock(VeilarbpersonClient.class);
    private static final VeilarbregistreringClient registreringClient = mock(VeilarbregistreringClient.class);
    private static final VeilarbvedtakinfoClient egenvurderingClient = mock(VeilarbvedtakinfoClient.class);
    private static final VeilarbdokumentClient veilarbdokumentClient = mock(VeilarbdokumentClient.class);
    private static final AktorregisterClient aktorregisterClient = mock(AktorregisterClient.class);
    private static final VeilarbarenaClient veilarbarenaClient = mock(VeilarbarenaClient.class);
    private static final AbacClient abacClient = mock(AbacClient.class);
    private static final DokarkivClient dokarkivClient = mock(DokarkivClient.class);
    private static final DokdistribusjonClient dokdistribusjonClient = mock(DokdistribusjonClient.class);

    private static final VeilarbPep veilarbPep = mock(VeilarbPep.class);
    private static final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

    private static final String CV_DATA = "{\"cv\": \"cv\"}";
    private static final String REGISTRERING_DATA = "{\"registrering\": \"registrering\"}";
    private static final String EGENVURDERING_DATA = "{\"egenvurdering\": \"egenvurdering\"}";

    @BeforeClass
    public static void setupOnce() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        transactor = new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
        kilderRepository = spy(new KilderRepository(db));
        meldingRepository = spy(new MeldingRepository(db));
        vedtaksstotteRepository = new VedtaksstotteRepository(db, transactor);
        oyeblikksbildeRepository = new OyeblikksbildeRepository(db);
        beslutteroversiktRepository = new BeslutteroversiktRepository(db);

        authService = spy(new AuthService(aktorregisterClient, veilarbPep, veilarbarenaClient, abacClient, null));
        oyeblikksbildeService = new OyeblikksbildeService(authService, oyeblikksbildeRepository, vedtaksstotteRepository, veilarbpersonClient, registreringClient, egenvurderingClient);
        malTypeService = new MalTypeService(registreringClient);
        dokumentServiceV2 = new DokumentServiceV2(
                veilarbdokumentClient, veilarbarenaClient, veilarbpersonClient, dokarkivClient, dokdistribusjonClient);
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
                dokumentServiceV2
        );
    }

    @Before
    public void setup() {
        DbTestUtils.cleanupDb(db);
        reset(veilederService);
        reset(veilarbdokumentClient);
        reset(meldingRepository);
        reset(unleashService);
        reset(dokdistribusjonClient);
        reset(dokarkivClient);
        reset(vedtakStatusEndringService);
        doReturn(TEST_VEILEDER_IDENT).when(authService).getInnloggetVeilederIdent();
        when(veilederService.hentEnhetNavn(TEST_OPPFOLGINGSENHET_ID)).thenReturn(TEST_OPPFOLGINGSENHET_NAVN);
        when(veilederService.hentVeileder(TEST_VEILEDER_IDENT)).thenReturn(new Veileder().setIdent(TEST_VEILEDER_IDENT).setNavn(TEST_VEILEDER_NAVN));
        when(veilarbdokumentClient.sendDokument(any())).thenReturn(new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID));
        when(veilarbdokumentClient.produserDokumentV2(any())).thenReturn("dokument".getBytes());
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(new AsyncResult(null));
        when(veilarbpersonClient.hentCVOgJobbprofil(TEST_FNR)).thenReturn(CV_DATA);
        when(registreringClient.hentRegistreringDataJson(TEST_FNR)).thenReturn(REGISTRERING_DATA);
        when(egenvurderingClient.hentEgenvurdering(TEST_FNR)).thenReturn(EGENVURDERING_DATA);
        when(aktorregisterClient.hentAktorId(Fnr.of(TEST_FNR))).thenReturn(AktorId.of(TEST_AKTOR_ID));
        when(aktorregisterClient.hentFnr(AktorId.of(TEST_AKTOR_ID))).thenReturn(Fnr.of(TEST_FNR));
        when(veilarbarenaClient.oppfolgingsenhet(Fnr.of(TEST_FNR))).thenReturn(EnhetId.of(TEST_OPPFOLGINGSENHET_ID));
        when(veilarbarenaClient.oppfolgingssak(Fnr.of(TEST_FNR))).thenReturn(TEST_OPPFOLGINGSSAK);
        when(veilarbpersonClient.hentPersonNavn(TEST_FNR)).thenReturn(new PersonNavn("Fornavn", null, "Etternavn", null));
        when(dokarkivClient.opprettJournalpost(any()))
                .thenReturn(new OpprettetJournalpostDTO(
                        TEST_JOURNALPOST_ID,
                        true,
                        Arrays.asList(new OpprettetJournalpostDTO.DokumentInfoId(TEST_DOKUMENT_ID))));
    }


    @Test
    public void fattVedtak__opprett_oppdater_og_send_vedtak() {
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

        when(dokdistribusjonClient.distribuerJournalpost(any()))
                .thenReturn(new DistribuerJournalpostResponsDTO(TEST_DOKUMENT_BESTILLING_ID));

        fattVedtak();

        assertSendtVedtakV2();
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
    public void fattVedtakV2__ferdigstiller_vedtak_og_sender_metrikk_for_manuell_retting_dersom_journalpost_ikke_er_ferdigstilt() {
        gittVersjon2AvFattVedtak();
        gittUtkastKlarForUtsendelse();

        when(dokarkivClient.opprettJournalpost(any()))
                .thenReturn(new OpprettetJournalpostDTO(
                        TEST_JOURNALPOST_ID,
                        false,
                        Arrays.asList(new OpprettetJournalpostDTO.DokumentInfoId(TEST_DOKUMENT_ID))));
        when(dokdistribusjonClient.distribuerJournalpost(any()))
                .thenReturn(new DistribuerJournalpostResponsDTO(TEST_DOKUMENT_BESTILLING_ID));

        fattVedtak();

        assertSendtVedtakV2();

        verify(metricsService).rapporterFeilendeFerdigstillingAvJournalpost();
    }

    private void fattVedtak() {
        withContext(() -> {
            gittTilgang();
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);
            vedtakService.fattVedtak(utkast.getId());
        });
    }

    @Test
    public void fattVedtakV2__ferdigstiller_vedtak_og_sender_metrikk_for_manuell_retting_dersom_distribusjon_feiler() {
        gittVersjon2AvFattVedtak();
        gittUtkastKlarForUtsendelse();

        when(dokdistribusjonClient.distribuerJournalpost(any()))
                .thenThrow(new RuntimeException());

        fattVedtak();
        assertSendtVedtak();

        verify(metricsService).rapporterFeilendeDistribusjonAvJournalpost();
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
    public void fattVedtak_v2_sender_ikke_mer_enn_en_gang() {
        when(dokdistribusjonClient.distribuerJournalpost(any()))
                .thenReturn(new DistribuerJournalpostResponsDTO(TEST_DOKUMENT_BESTILLING_ID));
        withContext(() -> {
            gittTilgang();
            gittUtkastKlarForUtsendelse();
            gittVersjon2AvFattVedtak();

            long id = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

            Stream<UnsafeSupplier<Future<?>>> stream = Stream.of(
                    (UnsafeSupplier<Future<?>>) () -> sendVedtakAsynk(id),
                    () -> sendVedtakAsynk(id),
                    () -> sendVedtakAsynk(id),
                    () -> sendVedtakAsynk(id)
            ).parallel();

            stream.forEach(f -> {
                try {
                    f.get().get();
                } catch (Exception ignored) {
                }
            });

            verify(dokarkivClient, times(1)).opprettJournalpost(any());
            verify(dokdistribusjonClient, times(1)).distribuerJournalpost(any());
        });
    }

    @Test
    @Ignore // Testen er ustabil på GHA
    public void fattVedtak_sender_ikke_mer_enn_en_gang() {
        withContext(() -> {
            gittTilgang();
            gittUtkastKlarForUtsendelse();

            long id = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

            Stream<UnsafeSupplier<Future<?>>> stream = Stream.of(
                    (UnsafeSupplier<Future<?>>) () -> sendVedtakAsynk(id),
                    () -> sendVedtakAsynk(id),
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

    @Test
    public void behandleOppfolgingsbrukerEndring_endrer_oppfolgingsenhet() {
        String nyEnhet = "4562";
        withContext(() -> {
            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

            vedtakService.behandleOppfolgingsbrukerEndring(new KafkaOppfolgingsbrukerEndring(TEST_AKTOR_ID, nyEnhet));

            Vedtak oppdatertUtkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);
            assertNotNull(oppdatertUtkast);
            assertEquals(nyEnhet, oppdatertUtkast.getOppfolgingsenhetId());
        });
    }

    private void gittTilgang() {
        when(veilarbPep.harVeilederTilgangTilPerson(any(NavIdent.class), any(), any())).thenReturn(true);
        when(veilarbPep.harVeilederTilgangTilEnhet(any(NavIdent.class), any())).thenReturn(true);
    }

    private void withContext(UnsafeRunnable runnable) {
        AuthContextHolder.withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, TEST_VEILEDER_IDENT), runnable);
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
        System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "dev-fss");
        when(unleashService.isNyDokIntegrasjonEnabled()).thenReturn(true);
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

    private void assertSendtVedtakV2() {
        assertSendtVedtak();
        withContext(() -> {
            gittTilgang();
            Vedtak sendtVedtak = hentVedtak();
            assertTrue(sendtVedtak.isGjeldende());
            assertEquals(TEST_DOKUMENT_BESTILLING_ID, sendtVedtak.getDokumentbestillingId());
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
            assertTrue(sendtVedtak.isGjeldende());
            assertFalse(sendtVedtak.isSender());

            List<Oyeblikksbilde> oyeblikksbilde = oyeblikksbildeService.hentOyeblikksbildeForVedtak(sendtVedtak.getId());
            assertThat(oyeblikksbilde, containsInAnyOrder(
                    equalTo(new Oyeblikksbilde(sendtVedtak.getId(), OyeblikksbildeType.REGISTRERINGSINFO, REGISTRERING_DATA)),
                    equalTo(new Oyeblikksbilde(sendtVedtak.getId(), OyeblikksbildeType.CV_OG_JOBBPROFIL, CV_DATA)),
                    equalTo(new Oyeblikksbilde(sendtVedtak.getId(), OyeblikksbildeType.EGENVURDERING, EGENVURDERING_DATA)))
            );
        });
    }

    ExecutorService executorService = Executors.newFixedThreadPool(3);

    private Future<?> sendVedtakAsynk(long id) {
        return executorService.submit(() -> {
            when(veilarbdokumentClient.sendDokument(any())).thenAnswer(invocation -> {
                Thread.sleep(10); // Simuler tregt API for v1
                return new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID);
            });

            when(dokarkivClient.opprettJournalpost(any())).thenAnswer(invocation -> {
                Thread.sleep(10); // Simuler tregt API for v2
                return new OpprettetJournalpostDTO(TEST_JOURNALPOST_ID, true, List.of(new OpprettetJournalpostDTO.DokumentInfoId(TEST_DOKUMENT_ID)));
            });
            withContext(() -> {
                vedtakService.fattVedtak(id);
            });
        });
    }
}

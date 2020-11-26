package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.abac.AbacClient;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.pdl.AktorOppslagClient;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.veilarbvedtaksstotte.client.api.*;
import no.nav.veilarbvedtaksstotte.client.api.dokument.DokumentClient;
import no.nav.veilarbvedtaksstotte.client.api.dokument.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.client.api.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.api.registrering.RegistreringClient;
import no.nav.veilarbvedtaksstotte.client.api.veilederogenhet.Veileder;
import no.nav.veilarbvedtaksstotte.domain.*;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.enums.OyeblikksbildeType;
import no.nav.veilarbvedtaksstotte.domain.enums.VedtakStatus;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    private static VeilarbpersonClient veilarbpersonClient = mock(VeilarbpersonClient.class);
    private static RegistreringClient registreringClient = mock(RegistreringClient.class);
    private static EgenvurderingClient egenvurderingClient = mock(EgenvurderingClient.class);
    private static VeilederService veilederService = mock(VeilederService.class);
    private static KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private static DokumentClient dokumentClient = mock(DokumentClient.class);
    private static VedtakStatusEndringService vedtakStatusEndringService = mock(VedtakStatusEndringService.class);
    private static AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);
    private static VeilarbPep veilarbPep = mock(VeilarbPep.class);
    private static ArenaClient arenaClient = mock(ArenaClient.class);
    private static AbacClient abacClient = mock(AbacClient.class);
    private static MetricsService metricsService = mock(MetricsService.class);

    private static String CV_DATA = "{\"cv\": \"cv\"}";
    private static String REGISTRERING_DATA = "{\"registrering\": \"registrering\"}";
    private static String EGENVURDERING_DATA = "{\"egenvurdering\": \"egenvurdering\"}";

    @BeforeClass
    public static void setupOnce() {
        db = SingletonPostgresContainer.init().getDb();
        transactor = new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
        kilderRepository = new KilderRepository(db);
        meldingRepository = spy(new MeldingRepository(db));
        vedtaksstotteRepository = new VedtaksstotteRepository(db, transactor);
        oyeblikksbildeRepository = new OyeblikksbildeRepository(db);
        beslutteroversiktRepository = new BeslutteroversiktRepository(db);

        authService = spy(new AuthService(aktorOppslagClient, veilarbPep, arenaClient, abacClient, null));
        oyeblikksbildeService = new OyeblikksbildeService(authService, oyeblikksbildeRepository, vedtaksstotteRepository, veilarbpersonClient, registreringClient, egenvurderingClient);
        malTypeService = new MalTypeService(registreringClient);
        vedtakService = new VedtakService(
                vedtaksstotteRepository,
                kilderRepository,
                oyeblikksbildeService,
                meldingRepository,
                beslutteroversiktRepository,
                authService,
                dokumentClient,
                null,
                veilederService,
                malTypeService,
                vedtakStatusEndringService,
                metricsService,
                transactor
        );
    }

    @Before
    public void setup() {
        DbTestUtils.cleanupDb(db);
        reset(veilederService);
        doReturn(TEST_VEILEDER_IDENT).when(authService).getInnloggetVeilederIdent();
        when(veilederService.hentEnhetNavn(TEST_OPPFOLGINGSENHET_ID)).thenReturn(TEST_OPPFOLGINGSENHET_NAVN);
        when(veilederService.hentVeileder(TEST_VEILEDER_IDENT)).thenReturn(new Veileder().setIdent(TEST_VEILEDER_IDENT).setNavn(TEST_VEILEDER_NAVN));
        reset(dokumentClient);
        when(dokumentClient.sendDokument(any())).thenReturn(new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(new AsyncResult(null));
        when(veilarbpersonClient.hentCVOgJobbprofil(TEST_FNR)).thenReturn(CV_DATA);
        when(registreringClient.hentRegistreringDataJson(TEST_FNR)).thenReturn(REGISTRERING_DATA);
        when(egenvurderingClient.hentEgenvurdering(TEST_FNR)).thenReturn(EGENVURDERING_DATA);
        when(aktorOppslagClient.hentAktorId(Fnr.of(TEST_FNR))).thenReturn(AktorId.of(TEST_AKTOR_ID));
        when(aktorOppslagClient.hentFnr(AktorId.of(TEST_AKTOR_ID))).thenReturn(Fnr.of(TEST_FNR));
        when(arenaClient.oppfolgingsenhet(TEST_FNR)).thenReturn(TEST_OPPFOLGINGSENHET_ID);
    }


    @Test
    public void fattVedtak__opprett_oppdater_og_send_vedtak() {
        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);
            assertNyttUtkast();
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);
            kilderRepository.lagKilder(TEST_KILDER, utkast.getId());

            VedtakDTO oppdaterDto = new VedtakDTO()
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

    private void assertOppdatertUtkast(VedtakDTO dto) {
        Vedtak oppdatertUtkast = vedtakService.hentUtkast(TEST_FNR);
        assertEquals(dto.getHovedmal(), oppdatertUtkast.getHovedmal());
        assertEquals(dto.getBegrunnelse(), oppdatertUtkast.getBegrunnelse());
        assertEquals(dto.getInnsatsgruppe(), oppdatertUtkast.getInnsatsgruppe());
        assertThat(oppdatertUtkast.getOpplysninger(), containsInAnyOrder(dto.getOpplysninger().toArray(new String[0])));
    }

    private void assertSendtVedtak() {
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
    public void oppdaterUtkast__feiler_for_veileder_som_ikke_er_satt_pa_utkast() {
        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT + "annen");

            assertThatThrownBy(() -> {
                vedtakService.fattVedtak(utkast.getId());
                vedtakService.oppdaterUtkast(utkast.getId(), new VedtakDTO());
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

        vedtakService.slettUtkast(TEST_AKTOR_ID);

        assertNull(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID));
    }

    @Test
    public void slettUtkast__feiler_for_veileder_som_ikke_er_satt_pa_utkast() {
        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);

            when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT + "annen");

            assertThatThrownBy(() ->
                    vedtakService.slettUtkast(TEST_FNR)
            ).isExactlyInstanceOf(ResponseStatusException.class);
        });
    }

    @Test
    public void fattVedtak__feiler_for_veileder_som_ikke_er_satt_pa_utkast() {
        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);

            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            vedtakService.oppdaterUtkast(utkast.getId(),
                    new VedtakDTO()
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
    @Ignore // TODO: Denne testen brekker pipelinen altfor ofte, må fikses
    public void fattVedtak_sender_ikke_mer_enn_en_gang() {
        withContext(() -> {
            gittTilgang();
            gittUtkastKlarForUtsendelse();

            Future<?> submit1 = sendVedtakAsynk();
            Future<?> submit2 = sendVedtakAsynk();

            assertThatThrownBy(() -> {
                submit1.get();
                submit2.get();
            }).matches(x -> x instanceof ExecutionException && x.getCause() instanceof IllegalStateException);

            verify(dokumentClient, times(1)).sendDokument(any());
        });
    }

    @Test
    public void fattVedtak__korrekt_sender_tilstand_dersom_send_dokument_feiler() {
        when(dokumentClient.sendDokument(any())).thenThrow(new RuntimeException());

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

    private void withContext(UnsafeRunnable runnable) { ;
        AuthContextHolder.withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, TEST_VEILEDER_IDENT), runnable);
    }

    private void gittUtkastKlarForUtsendelse() {
        withContext(() -> {
            gittTilgang();
            vedtakService.lagUtkast(TEST_FNR);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            VedtakDTO oppdaterDto = new VedtakDTO()
                    .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                    .setBegrunnelse("En begrunnelse")
                    .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                    .setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

            List<String> kilder = List.of("opp1", "opp2");

            kilderRepository.lagKilder(kilder, utkast.getId());

            vedtakService.oppdaterUtkast(utkast.getId(), oppdaterDto);

        });
    }

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    private Future<?> sendVedtakAsynk() {
        return executorService.submit(() -> {
            when(dokumentClient.sendDokument(any())).thenAnswer(invocation -> {
                Thread.sleep(1000); // Simuler tregt API for å sende dokument
                return new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID);
            });
            withContext(() -> {
                Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);
                vedtakService.fattVedtak(utkast.getId());
            });
        });
    }
}

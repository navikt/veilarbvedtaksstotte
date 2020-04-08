package no.nav.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.jdbc.Transactor;
import no.nav.sbl.util.fn.UnsafeRunnable;
import no.nav.veilarbvedtaksstotte.client.*;
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

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.*;
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
    private static Transactor transactor;

    private static VedtaksstotteRepository vedtaksstotteRepository;
    private static KilderRepository kilderRepository;
    private static MeldingRepository meldingRepository;
    private static OyeblikksbildeRepository oyeblikksbildeRepository;
    private static BeslutteroversiktRepository beslutteroversiktRepository;

    private static VedtakService vedtakService;
    private static OyeblikksbildeService oyeblikksbildeService;
    private static MalTypeService malTypeService;
    private static AuthService authService;

    private static CVClient cvClient = mock(CVClient.class);
    private static RegistreringClient registreringClient = mock(RegistreringClient.class);
    private static EgenvurderingClient egenvurderingClient = mock(EgenvurderingClient.class);
    private static VeilederService veilederService = mock(VeilederService.class);
    private static KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private static DokumentClient dokumentClient = mock(DokumentClient.class);
    private static VedtakStatusEndringService vedtakStatusEndringService = mock(VedtakStatusEndringService.class);
    private static AktorService aktorService = mock(AktorService.class);
    private static PepClient pepClient = mock(PepClient.class);
    private static Pep pep = mock(Pep.class);
    private static ArenaClient arenaClient = mock(ArenaClient.class);

    private static String CV_DATA = "{\"cv\": \"cv\"}";
    private static String REGISTRERING_DATA = "{\"registrering\": \"registrering\"}";
    private static String EGENVURDERING_DATA = "{\"egenvurdering\": \"egenvurdering\"}";

    @BeforeClass
    public static void setupOnce() {
        db = SingletonPostgresContainer.init().getDb();
        transactor = new Transactor(new DataSourceTransactionManager(db.getDataSource()));
        kilderRepository = new KilderRepository(db);
        meldingRepository = spy(new MeldingRepository(db));
        vedtaksstotteRepository = new VedtaksstotteRepository(db, kilderRepository, transactor);
        oyeblikksbildeRepository = new OyeblikksbildeRepository(db);
        beslutteroversiktRepository = new BeslutteroversiktRepository(db);

        authService = spy(new AuthService(aktorService, pepClient, pep, arenaClient));
        oyeblikksbildeService = new OyeblikksbildeService(authService, oyeblikksbildeRepository, cvClient, registreringClient, egenvurderingClient);
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
        when(cvClient.hentCV(TEST_FNR)).thenReturn(CV_DATA);
        when(registreringClient.hentRegistreringDataJson(TEST_FNR)).thenReturn(REGISTRERING_DATA);
        when(egenvurderingClient.hentEgenvurdering(TEST_FNR)).thenReturn(EGENVURDERING_DATA);
        when(aktorService.getAktorId(TEST_FNR)).thenReturn(Optional.of(TEST_AKTOR_ID));
        when(arenaClient.oppfolgingsenhet(TEST_FNR)).thenReturn(TEST_OPPFOLGINGSENHET_ID);
    }


    @Test
    public void sendVedtak__opprett_oppdater_og_send_vedtak() {
        withSubject(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);
            assertNyttUtkast();

            VedtakDTO oppdaterDto = new VedtakDTO()
                    .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                    .setBegrunnelse("En begrunnelse")
                    .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                    .setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

            vedtakService.oppdaterUtkast(TEST_FNR, oppdaterDto);
            assertOppdatertUtkast(oppdaterDto);

            vedtakService.sendVedtak(TEST_FNR);
            assertSendtVedtak();
        });
    }

    private void assertNyttUtkast() {
        Vedtak opprettetUtkast = hentVedtak();
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
        List<Vedtak> vedtakList = vedtakService.hentVedtak(TEST_FNR);
        assertEquals(vedtakList.size(), 1);
        return vedtakList.get(0);
    }

    private void assertOppdatertUtkast(VedtakDTO dto) {
        Vedtak oppdatertUtkast = hentVedtak();
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

        List<Oyeblikksbilde> oyeblikksbilde = oyeblikksbildeService.hentOyeblikksbildeForVedtak(TEST_FNR, sendtVedtak.getId());
        assertThat(oyeblikksbilde, containsInAnyOrder(
                equalTo(new Oyeblikksbilde(sendtVedtak.getId(), OyeblikksbildeType.REGISTRERINGSINFO, REGISTRERING_DATA)),
                equalTo(new Oyeblikksbilde(sendtVedtak.getId(), OyeblikksbildeType.CV_OG_JOBBPROFIL, CV_DATA)),
                equalTo(new Oyeblikksbilde(sendtVedtak.getId(), OyeblikksbildeType.EGENVURDERING, EGENVURDERING_DATA)))
        );
    }

    @Test
    public void lagUtkast__skal_opprette_system_melding() {
        withSubject(() -> {
            gittTilgang();
            vedtakService.lagUtkast(TEST_FNR);

            verify(meldingRepository, times(1))
                    .opprettSystemMelding(anyLong(), eq(SystemMeldingType.UTKAST_OPPRETTET), eq(TEST_VEILEDER_IDENT));
        });
    }


    @Test
    public void oppdaterUtkast__feiler_for_veileder_som_ikke_er_satt_pa_utkast() {
        withSubject(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);

            when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT + "annen");

            assertThatThrownBy(() ->
                    vedtakService.oppdaterUtkast(TEST_FNR, new VedtakDTO())
            ).isExactlyInstanceOf(IngenTilgang.class);
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
        withSubject(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);

            when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT + "annen");

            assertThatThrownBy(() ->
                    vedtakService.slettUtkastForFnr(TEST_FNR)
            ).isExactlyInstanceOf(IngenTilgang.class);
        });
    }

    @Test
    public void sendVedtak__feiler_for_veileder_som_ikke_er_satt_pa_utkast() {
        withSubject(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);
            vedtakService.oppdaterUtkast(TEST_FNR,
                    new VedtakDTO()
                            .setBegrunnelse("begrunnelse")
                            .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                            .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                            .setOpplysninger(Collections.singletonList("opplysning")));

            when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT + "annen");

            assertThatThrownBy(() ->
                    vedtakService.sendVedtak(TEST_FNR)
            ).isExactlyInstanceOf(IngenTilgang.class);
        });
    }


    @Test
    @Ignore // TODO: Denne testen brekker pipelinen altfor ofte, må fikses
    public void sendVedtak__sender_ikke_mer_enn_en_gang() {
        withSubject(() -> {
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
    public void sendVedtak__korrekt_sender_tilstand_dersom_send_dokument_feiler() {
        when(dokumentClient.sendDokument(any())).thenThrow(new RuntimeException());

        gittUtkastKlarForUtsendelse();

        withSubject(() -> {
            gittTilgang();
            assertThatThrownBy(() ->
            vedtakService.sendVedtak(TEST_FNR)).isExactlyInstanceOf(RuntimeException.class);

            assertFalse(hentVedtak().isSender());
        });
    }

    @Test
    public void taOverUtkast__setter_ny_veileder() {
        withSubject(() -> {
            String tidligereVeilederId = TEST_VEILEDER_IDENT + "tidligere";
            gittTilgang();
            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, tidligereVeilederId, TEST_OPPFOLGINGSENHET_ID);

            assertEquals(tidligereVeilederId, vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getVeilederIdent());
            vedtakService.taOverUtkast(TEST_FNR);
            assertEquals(TEST_VEILEDER_IDENT, vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getVeilederIdent());
        });
    }

    @Test
    public void taOverUtkast__oppretter_system_melding() {
        withSubject(() -> {
            String tidligereVeilederId = TEST_VEILEDER_IDENT + "tidligere";
            gittTilgang();
            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, tidligereVeilederId, TEST_OPPFOLGINGSENHET_ID);

            reset(meldingRepository);

            vedtakService.taOverUtkast(TEST_FNR);

            verify(meldingRepository, times(1))
                    .opprettSystemMelding(anyLong(), eq(SystemMeldingType.TATT_OVER_SOM_VEILEDER), eq(TEST_VEILEDER_IDENT));
        });
    }

    @Test
    public void taOverUtkast__feiler_dersom_ikke_utkast() {
        withSubject(() -> {
            gittTilgang();

            assertThatThrownBy(() ->
                    vedtakService.taOverUtkast(TEST_FNR)
            ).isExactlyInstanceOf(NotFoundException.class);
        });
    }

    @Test
    public void taOverUtkast__feiler_dersom_ingen_tilgang() {
        when(pepClient.sjekkLesetilgangTilAktorId(TEST_AKTOR_ID)).thenThrow(new IngenTilgang());

        assertThatThrownBy(() ->
                vedtakService.taOverUtkast(TEST_FNR)
        ).isExactlyInstanceOf(IngenTilgang.class);
    }

    @Test
    public void taOverUtkast__feiler_dersom_samme_veileder() {
        withSubject(() -> {
            gittTilgang();
            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

            assertThatThrownBy(() ->
                    vedtakService.taOverUtkast(TEST_FNR)
            ).isExactlyInstanceOf(BadRequestException.class);
        });
    }

    @Test
    public void behandleOppfolgingsbrukerEndring_endrer_oppfolgingsenhet() {
        String nyEnhet = "4562";
        when(pepClient.harTilgangTilEnhet(TEST_OPPFOLGINGSENHET_ID)).thenReturn(true);
        when(pepClient.harTilgangTilEnhet(nyEnhet)).thenReturn(true);
        withSubject(() -> {
            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

            vedtakService.behandleOppfolgingsbrukerEndring(new KafkaOppfolgingsbrukerEndring(TEST_AKTOR_ID, nyEnhet));

            List<Vedtak> oppdatertUtkastListe = vedtakService.hentVedtak(TEST_FNR);
            assertEquals(oppdatertUtkastListe.size(), 1);
            assertEquals(nyEnhet, oppdatertUtkastListe.get(0).getOppfolgingsenhetId());
        });
    }

    private void gittTilgang() {
        when(pepClient.harTilgangTilEnhet(TEST_OPPFOLGINGSENHET_ID)).thenReturn(true);
    }

    private void withSubject(UnsafeRunnable runnable) {
        SubjectHandler.withSubject(new Subject(TEST_VEILEDER_IDENT, IdentType.InternBruker, SsoToken.oidcToken("token", new HashMap<>())), runnable);
    }

    private void gittUtkastKlarForUtsendelse() {
        withSubject(() -> {
            gittTilgang();
            vedtakService.lagUtkast(TEST_FNR);

            VedtakDTO oppdaterDto = new VedtakDTO()
                    .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                    .setBegrunnelse("En begrunnelse")
                    .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                    .setOpplysninger(Arrays.asList("opplysning 1", "opplysning 2"));

            vedtakService.oppdaterUtkast(TEST_FNR, oppdaterDto);

        });
    }

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    private Future<?> sendVedtakAsynk() {
        return executorService.submit(() -> {
            when(dokumentClient.sendDokument(any())).thenAnswer(invocation -> {
                Thread.sleep(1000); // Simuler tregt API for å sende dokument
                return new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID);
            });
            withSubject(() -> {
                vedtakService.sendVedtak(TEST_FNR);
            });
        });
    }
}

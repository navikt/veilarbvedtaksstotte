package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbvedtaksstotte.client.*;
import no.nav.fo.veilarbvedtaksstotte.domain.*;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.OyeblikksbildeType;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.VedtakStatus;
import no.nav.fo.veilarbvedtaksstotte.kafka.VedtakSendtTemplate;
import no.nav.fo.veilarbvedtaksstotte.kafka.VedtakStatusEndringTemplate;
import no.nav.fo.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.KilderRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.OyeblikksbildeRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.fo.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import no.nav.sbl.jdbc.Transactor;
import no.nav.sbl.util.fn.UnsafeRunnable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.AsyncResult;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static no.nav.fo.veilarbvedtaksstotte.utils.TestData.*;
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
    private static OyeblikksbildeRepository oyeblikksbildeRepository;
    private static KafkaRepository kafkaRepository;

    private static VedtakSendtTemplate vedtakSendtTemplate;
    private static VedtakStatusEndringTemplate vedtakStatusEndringTemplate;

    private static VedtakService vedtakService;
    private static OyeblikksbildeService oyeblikksbildeService;
    private static MalTypeService malTypeService;
    private static KafkaService kafkaService;
    private static AuthService authSerivce;

    private static CVClient cvClient = mock(CVClient.class);
    private static RegistreringClient registreringClient = mock(RegistreringClient.class);
    private static EgenvurderingClient egenvurderingClient = mock(EgenvurderingClient.class);
    private static VeilederService veilederService = mock(VeilederService.class);
    private static KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private static DokumentClient dokumentClient = mock(DokumentClient.class);
    private static MetricsService metricsService = mock(MetricsService.class);
    private static AktorService aktorService = mock(AktorService.class);
    private static PepClient pepClient = mock(PepClient.class);
    private static ArenaClient arenaClient = mock(ArenaClient.class);

    private static String CV_DATA = "{\"cv\": \"cv\"}";
    private static String REGISTRERING_DATA = "{\"registrering\": \"registrering\"}";
    private static String EGENVURDERING_DATA = "{\"egenvurdering\": \"egenvurdering\"}";

    @BeforeClass
    public static void setupOnce() {
        db = SingletonPostgresContainer.init().getDb();
        transactor = new Transactor(new DataSourceTransactionManager(db.getDataSource()));
        kilderRepository = new KilderRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, kilderRepository);
        oyeblikksbildeRepository = new OyeblikksbildeRepository(db);
        kafkaRepository = new KafkaRepository(db);

        vedtakSendtTemplate = new VedtakSendtTemplate(kafkaTemplate, "vedtakSendt", kafkaRepository);
        vedtakStatusEndringTemplate = new VedtakStatusEndringTemplate(kafkaTemplate, "vedtakStatusEndring", kafkaRepository);

        authSerivce = new AuthService(aktorService, pepClient, arenaClient, veilederService);
        oyeblikksbildeService = new OyeblikksbildeService(authSerivce, oyeblikksbildeRepository, cvClient, registreringClient, egenvurderingClient);
        malTypeService = new MalTypeService(registreringClient);
        kafkaService = new KafkaService(vedtakSendtTemplate, vedtakStatusEndringTemplate, vedtaksstotteRepository);
        vedtakService = new VedtakService(vedtaksstotteRepository,
                kilderRepository,
                oyeblikksbildeService,
                authSerivce,
                dokumentClient,
                null,
                veilederService,
                malTypeService,
                kafkaService,
                metricsService,
                transactor);
    }

    @Before
    public void setup() {
        DbTestUtils.cleanupDb(db);
        reset(veilederService);
        when(veilederService.hentVeilederIdentFraToken()).thenReturn(TEST_VEILEDER_IDENT);
        when(veilederService.hentEnhetNavn(TEST_OPPFOLGINGSENHET_ID)).thenReturn(TEST_OPPFOLGINGSENHET_NAVN);
        when(veilederService.hentVeileder(TEST_VEILEDER_IDENT)).thenReturn(new Veileder().setIdent(TEST_VEILEDER_IDENT).setNavn(TEST_VEILEDER_NAVN));
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

            vedtakService.sendVedtak(TEST_FNR, TEST_BESLUTTER);
            assertSendtVedtak();
        });
    }

    private void assertNyttUtkast() {
        List<Vedtak> opprettetUtkastListe = vedtakService.hentVedtak(TEST_FNR);
        assertEquals(opprettetUtkastListe.size(), 1);
        Vedtak opprettetUtkast = opprettetUtkastListe.get(0);
        assertEquals(VedtakStatus.UTKAST, opprettetUtkast.getVedtakStatus());
        assertEquals(TEST_VEILEDER_IDENT, opprettetUtkast.getVeilederIdent());
        assertEquals(TEST_VEILEDER_NAVN, opprettetUtkast.getVeilederNavn());
        assertEquals(TEST_OPPFOLGINGSENHET_ID, opprettetUtkast.getOppfolgingsenhetId());
        assertEquals(TEST_OPPFOLGINGSENHET_NAVN, opprettetUtkast.getOppfolgingsenhetNavn());
        assertFalse(opprettetUtkast.isGjeldende());
        assertFalse(opprettetUtkast.isSendtTilBeslutter());
        assertEquals(opprettetUtkast.getOpplysninger().size(), 0);
    }

    private void assertOppdatertUtkast(VedtakDTO dto) {
        List<Vedtak> oppdatertUtkastListe = vedtakService.hentVedtak(TEST_FNR);
        assertEquals(oppdatertUtkastListe.size(), 1);
        Vedtak oppdatertUtkast = oppdatertUtkastListe.get(0);
        assertEquals(dto.getHovedmal(), oppdatertUtkast.getHovedmal());
        assertEquals(dto.getBegrunnelse(), oppdatertUtkast.getBegrunnelse());
        assertEquals(dto.getInnsatsgruppe(), oppdatertUtkast.getInnsatsgruppe());
        assertThat(oppdatertUtkast.getOpplysninger(), containsInAnyOrder(dto.getOpplysninger().toArray(new String[0])));
    }

    private void assertSendtVedtak() {
        List<Vedtak> sendtVedtakListe = vedtakService.hentVedtak(TEST_FNR);
        assertEquals(sendtVedtakListe.size(), 1);
        Vedtak sendtVedtak = sendtVedtakListe.get(0);
        assertEquals(VedtakStatus.SENDT, sendtVedtak.getVedtakStatus());
        assertEquals(TEST_DOKUMENT_ID, sendtVedtak.getDokumentInfoId());
        assertEquals(TEST_JOURNALPOST_ID, sendtVedtak.getJournalpostId());
        assertEquals(TEST_BESLUTTER, sendtVedtak.getBeslutterNavn());
        assertTrue(sendtVedtak.isGjeldende());

        List<Oyeblikksbilde> oyeblikksbilde = oyeblikksbildeService.hentOyeblikksbildeForVedtak(TEST_FNR, sendtVedtak.getId());
        assertThat(oyeblikksbilde, containsInAnyOrder(
                equalTo(new Oyeblikksbilde(sendtVedtak.getId(), OyeblikksbildeType.REGISTRERINGSINFO, REGISTRERING_DATA)),
                equalTo(new Oyeblikksbilde(sendtVedtak.getId(), OyeblikksbildeType.CV_OG_JOBBPROFIL, CV_DATA)),
                equalTo(new Oyeblikksbilde(sendtVedtak.getId(), OyeblikksbildeType.EGENVURDERING, EGENVURDERING_DATA)))
        );
    }

    @Test
    public void oppdaterUtkast__feiler_for_veileder_som_ikke_er_satt_pa_utkast() {
        withSubject(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);

            when(veilederService.hentVeilederIdentFraToken()).thenReturn(TEST_VEILEDER_IDENT + "annen");

            assertThatThrownBy(() ->
                    vedtakService.oppdaterUtkast(TEST_FNR, new VedtakDTO())
            ).isExactlyInstanceOf(IngenTilgang.class);
        });
    }

    @Test
    public void slettUtkast__feiler_for_veileder_som_ikke_er_satt_pa_utkast() {
        withSubject(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);

            when(veilederService.hentVeilederIdentFraToken()).thenReturn(TEST_VEILEDER_IDENT + "annen");

            assertThatThrownBy(() ->
                    vedtakService.slettUtkast(TEST_FNR)
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
                            .setOpplysninger(Arrays.asList("opplysning")));

            when(veilederService.hentVeilederIdentFraToken()).thenReturn(TEST_VEILEDER_IDENT + "annen");

            assertThatThrownBy(() ->
                    vedtakService.sendVedtak(TEST_FNR, TEST_BESLUTTER)
            ).isExactlyInstanceOf(IngenTilgang.class);
        });
    }

    @Test
    public void taOverUtkast__setter_ny_veileder() {
        withSubject(() -> {
            String tidligereVeilederId = TEST_VEILEDER_IDENT + "tidligere";
            gittTilgang();
            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, tidligereVeilederId, TEST_OPPFOLGINGSENHET_ID, TEST_OPPFOLGINGSENHET_NAVN);

            assertEquals(tidligereVeilederId, vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getVeilederIdent());
            vedtakService.taOverUtkast(TEST_FNR);
            assertEquals(TEST_VEILEDER_IDENT, vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getVeilederIdent());
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
            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID, TEST_OPPFOLGINGSENHET_NAVN);

            assertThatThrownBy(() ->
                    vedtakService.taOverUtkast(TEST_FNR)
            ).isExactlyInstanceOf(BadRequestException.class);
        });
    }

    private void gittTilgang() {
        when(pepClient.harTilgangTilEnhet(TEST_OPPFOLGINGSENHET_ID)).thenReturn(true);
    }

    private void withSubject(UnsafeRunnable runnable) {
        SubjectHandler.withSubject(new Subject(TEST_VEILEDER_IDENT, IdentType.InternBruker, SsoToken.oidcToken("token", new HashMap<>())), runnable);
    }
}

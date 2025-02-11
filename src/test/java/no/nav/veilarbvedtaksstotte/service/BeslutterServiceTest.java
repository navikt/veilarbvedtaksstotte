package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.TilgangType;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.person.dto.PersonNavn;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.Veileder;
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.MeldingRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus.GODKJENT_AV_BESLUTTER;
import static no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus.KLAR_TIL_BESLUTTER;
import static no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus.KLAR_TIL_VEILEDER;
import static no.nav.veilarbvedtaksstotte.utils.TestData.SOME_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_AKTOR_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_BESLUTTER_IDENT;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_IKKE_ANSVARLIG_VEILEDER_IDENT;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSENHET_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSENHET_NAVN;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_NAVN;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BeslutterServiceTest {

    private final VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);

    private final VedtakHendelserService vedtakStatusEndringService = mock(VedtakHendelserService.class);

    private final BeslutteroversiktRepository beslutteroversiktRepository = mock(BeslutteroversiktRepository.class);

    private final MeldingRepository meldingRepository = mock(MeldingRepository.class);

    private final VeilederService veilederService = mock(VeilederService.class);

    private final VeilarbpersonClient veilarbpersonClient = mock(VeilarbpersonClient.class);

    private final MetricsService metricsService = mock(MetricsService.class);

    private final AuthService authService = mock(AuthService.class);

    private final SakStatistikkService sakStatistikkService = mock(SakStatistikkService.class);

    private final JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();

    private TransactionTemplate transactor = new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));

    private BeslutterService beslutterService = new BeslutterService(
            authService, vedtaksstotteRepository, vedtakStatusEndringService, beslutteroversiktRepository,
            meldingRepository, veilederService, veilarbpersonClient, transactor, metricsService, sakStatistikkService
    );


    private AuthKontekst authKontekst = new AuthKontekst("", TEST_AKTOR_ID, "");

    @BeforeEach
    public void setup() {
        doReturn(TEST_VEILEDER_IDENT).when(authService).getInnloggetVeilederIdent();
        doReturn(authKontekst).when(authService).sjekkTilgangTilBrukerOgEnhet(eq(TilgangType.SKRIVE), any(Fnr.class));
        doReturn(TEST_FNR).when(authService).getFnrOrThrow(TEST_AKTOR_ID);

        when(veilederService.hentVeileder(TEST_VEILEDER_IDENT)).thenReturn(new Veileder(TEST_VEILEDER_IDENT, TEST_VEILEDER_NAVN));
        when(veilederService.hentEnhetNavn(anyString())).thenReturn(TEST_OPPFOLGINGSENHET_NAVN);
        when(veilarbpersonClient.hentPersonNavn(TEST_FNR.get())).thenReturn(new PersonNavn("FORNAVN", null, "ETTERNAVN", null));
    }

    @Test
    public void startBeslutterProsess__skal_starte_beslutter_prosess_hvis_ikke_startet() {
        Vedtak vedtak = new Vedtak()
                .setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS)
                .setAktorId(TEST_AKTOR_ID)
                .setOppfolgingsenhetId(TEST_OPPFOLGINGSENHET_ID)
                .setVeilederIdent(TEST_VEILEDER_IDENT);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        beslutterService.startBeslutterProsess(SOME_ID);

        verify(vedtaksstotteRepository, times(1)).setBeslutterProsessStatus(anyLong(), eq(KLAR_TIL_BESLUTTER));
    }

    @Test
    public void startBeslutterProsess__skal_ikke_starte_beslutter_prosess_hvis_allerede_startet() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setBeslutterProsessStatus(BeslutterProsessStatus.KLAR_TIL_BESLUTTER)
                .setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.startBeslutterProsess(SOME_ID));
    }

    @Test
    public void startBeslutterProsess__skal_kaste_exception_hvis_feil_innsatsgruppe() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.startBeslutterProsess(SOME_ID));
    }

    @Test
    public void startBeslutterProsess__skal_opprette_system_melding() {
        Vedtak vedtak = new Vedtak()
                .setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS)
                .setAktorId(TEST_AKTOR_ID)
                .setOppfolgingsenhetId(TEST_OPPFOLGINGSENHET_ID)
                .setVeilederIdent(TEST_VEILEDER_IDENT);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        beslutterService.startBeslutterProsess(SOME_ID);

        verify(meldingRepository, times(1))
                .opprettSystemMelding(anyLong(), eq(SystemMeldingType.BESLUTTER_PROSESS_STARTET), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void avbrytBeslutterProsess__skal_avbryte_beslutter_prosess_hvis_ikke_allerede_gjort() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .setBeslutterProsessStatus(KLAR_TIL_BESLUTTER);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        beslutterService.avbrytBeslutterProsess(SOME_ID);

        verify(vedtaksstotteRepository, times(1)).setBeslutterProsessStatus(anyLong(), eq(null));
    }

    @Test
    public void avbrytBeslutterProsess__skal_kaste_exception_hvis_feil_innsatsgruppe() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.avbrytBeslutterProsess(SOME_ID));
    }

    @Test
    public void avbrytBeslutterProsess__skal_opprette_system_melding() {
        Vedtak vedtak = new Vedtak()
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .setBeslutterProsessStatus(KLAR_TIL_BESLUTTER)
                .setAktorId(TEST_AKTOR_ID)
                .setOppfolgingsenhetId(TEST_OPPFOLGINGSENHET_ID)
                .setVeilederIdent(TEST_VEILEDER_IDENT);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        beslutterService.avbrytBeslutterProsess(SOME_ID);

        verify(meldingRepository, times(1))
                .opprettSystemMelding(anyLong(), eq(SystemMeldingType.BESLUTTER_PROSESS_AVBRUTT), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void bliBeslutter__skal_ta_over_som_beslutter() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        beslutterService.bliBeslutter(SOME_ID);

        verify(vedtaksstotteRepository, times(1)).setBeslutter(anyLong(), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void bliBeslutter__skal_kaste_exception_nar_veileder_er_ansvarlig() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setVeilederIdent(TEST_VEILEDER_IDENT)
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.bliBeslutter(SOME_ID));
    }

    @Test
    public void bliBeslutter__skal_ikke_endres_hvis_allerede_beslutter() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setBeslutterIdent(TEST_VEILEDER_IDENT)
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.bliBeslutter(SOME_ID));
    }

    @Test
    public void bliBeslutter__skal_opprette_system_melding_bli_beslutter() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        beslutterService.bliBeslutter(SOME_ID);

        verify(meldingRepository, times(1))
                .opprettSystemMelding(anyLong(), eq(SystemMeldingType.BLITT_BESLUTTER), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void bliBeslutter__skal_opprette_system_melding_overta_for_beslutter() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .setBeslutterIdent("TIDLIGERE BESLUTTER IDENT");

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        beslutterService.bliBeslutter(SOME_ID);

        verify(meldingRepository, times(1))
                .opprettSystemMelding(anyLong(), eq(SystemMeldingType.TATT_OVER_SOM_BESLUTTER), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void godkjennVedtak__skal_sette_godkjent() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setBeslutterIdent(TEST_VEILEDER_IDENT)
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        beslutterService.setGodkjentAvBeslutter(SOME_ID);

        verify(vedtaksstotteRepository, times(1)).setBeslutterProsessStatus(anyLong(), eq(GODKJENT_AV_BESLUTTER));
    }

    @Test
    public void godkjennVedtak__skal_kaste_exception_hvis_ikke_beslutter() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setBeslutterIdent(TEST_BESLUTTER_IDENT)
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.setGodkjentAvBeslutter(SOME_ID));
    }

    @Test
    public void godkjennVedtak__skal_opprette_system_melding() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setBeslutterIdent(TEST_VEILEDER_IDENT)
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        beslutterService.setGodkjentAvBeslutter(SOME_ID);

        verify(meldingRepository, times(1))
                .opprettSystemMelding(anyLong(), eq(SystemMeldingType.BESLUTTER_HAR_GODKJENT), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void oppdaterBeslutterProsessStatus__skal_sette_riktig_status_for_veileder() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setBeslutterIdent(TEST_BESLUTTER_IDENT)
                .setVeilederIdent(TEST_VEILEDER_IDENT)
                .setBeslutterProsessStatus(KLAR_TIL_VEILEDER);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        beslutterService.oppdaterBeslutterProsessStatus(SOME_ID);

        verify(vedtaksstotteRepository, times(1)).setBeslutterProsessStatus(anyLong(), eq(KLAR_TIL_BESLUTTER));
    }

    @Test
    public void oppdaterBeslutterProsessStatus__skal_sette_riktig_status_for_beslutter() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setBeslutterIdent(TEST_BESLUTTER_IDENT)
                .setVeilederIdent(TEST_VEILEDER_IDENT)
                .setBeslutterProsessStatus(KLAR_TIL_BESLUTTER);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_BESLUTTER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        beslutterService.oppdaterBeslutterProsessStatus(SOME_ID);

        verify(vedtaksstotteRepository, times(1)).setBeslutterProsessStatus(anyLong(), eq(KLAR_TIL_VEILEDER));
    }

    @Test
    public void oppdaterBeslutterProsessStatus__skal_feile_hvis_status_er_lik() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setBeslutterIdent(TEST_BESLUTTER_IDENT)
                .setBeslutterProsessStatus(KLAR_TIL_VEILEDER);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_BESLUTTER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.oppdaterBeslutterProsessStatus(SOME_ID));
    }

    @Test
    public void oppdaterBeslutterProsessStatus__skal_feile_hvis_bruker_ikke_er_beslutter_eller_veileder() {
        Vedtak vedtak = new Vedtak()
                .setAktorId(TEST_AKTOR_ID)
                .setBeslutterIdent(TEST_BESLUTTER_IDENT)
                .setVeilederIdent(TEST_VEILEDER_IDENT)
                .setBeslutterProsessStatus(KLAR_TIL_VEILEDER);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, TEST_FNR)).thenReturn(authKontekst);
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_IKKE_ANSVARLIG_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.oppdaterBeslutterProsessStatus(SOME_ID));
    }
}

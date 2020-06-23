package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.client.api.PersonClient;
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.veilarbvedtaksstotte.domain.PersonNavn;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.Veileder;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutterProsessStatus;
import no.nav.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.MeldingRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.veilarbvedtaksstotte.domain.enums.BeslutterProsessStatus.*;
import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class BeslutterServiceTest {

    private VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);

    private VedtakStatusEndringService vedtakStatusEndringService = mock(VedtakStatusEndringService.class);

    private BeslutteroversiktRepository beslutteroversiktRepository = mock(BeslutteroversiktRepository.class);

    private MeldingRepository meldingRepository = mock(MeldingRepository.class);

    private VeilederService veilederService = mock(VeilederService.class);

    private PersonClient personClient = mock(PersonClient.class);

    private AuthService authService = mock(AuthService.class);

    private BeslutterService beslutterService = new BeslutterService(
            authService, vedtaksstotteRepository, vedtakStatusEndringService,
            beslutteroversiktRepository, meldingRepository, veilederService, personClient
    );

    @Before
    public void setup() {
        doReturn(TEST_VEILEDER_IDENT).when(authService).getInnloggetVeilederIdent();
        doReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID)).when(authService).sjekkTilgangTilFnr(anyString());
        doReturn(TEST_FNR).when(authService).getFnrOrThrow(TEST_AKTOR_ID);

        when(veilederService.hentVeileder(TEST_VEILEDER_IDENT)).thenReturn(new Veileder().setIdent(TEST_VEILEDER_IDENT).setNavn("VEILEDER"));
        when(veilederService.hentEnhetNavn(anyString())).thenReturn(TEST_OPPFOLGINGSENHET_NAVN);
        when(personClient.hentPersonNavn(TEST_FNR)).thenReturn(new PersonNavn().setFornavn("FORNAVN").setEtternavn("ETTERNAVN"));
    }

    @Test
    public void startBeslutterProsess__skal_starte_beslutter_prosess_hvis_ikke_startet() {
        Vedtak vedtak = new Vedtak()
                .setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS)
                .setAktorId(TEST_AKTOR_ID)
                .setOppfolgingsenhetId(TEST_OPPFOLGINGSENHET_ID)
                .setVeilederIdent(TEST_VEILEDER_IDENT);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        beslutterService.startBeslutterProsess(TEST_FNR);

        verify(vedtaksstotteRepository, times(1)).setBeslutterProsessStatus(anyLong(), eq(KLAR_TIL_BESLUTTER));
    }

    @Test
    public void startBeslutterProsess__skal_ikke_starte_beslutter_prosess_hvis_allerede_startet() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterProsessStatus(BeslutterProsessStatus.KLAR_TIL_BESLUTTER);
        vedtak.setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () ->  beslutterService.startBeslutterProsess(TEST_FNR));
    }

    @Test
    public void startBeslutterProsess__skal_kaste_exception_hvis_feil_innsatsgruppe() {
        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.startBeslutterProsess(TEST_FNR));
    }

    @Test
    public void startBeslutterProsess__skal_opprette_system_melding() {
        Vedtak vedtak = new Vedtak()
                .setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS)
                .setAktorId(TEST_AKTOR_ID)
                .setOppfolgingsenhetId(TEST_OPPFOLGINGSENHET_ID)
                .setVeilederIdent(TEST_VEILEDER_IDENT);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        beslutterService.startBeslutterProsess(TEST_FNR);

        verify(meldingRepository, times(1))
                .opprettSystemMelding(anyLong(), eq(SystemMeldingType.BESLUTTER_PROSESS_STARTET), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void bliBeslutter__skal_ta_over_som_beslutter() {
        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        beslutterService.bliBeslutter(TEST_FNR);

        verify(vedtaksstotteRepository, times(1)).setBeslutter(anyLong(), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void bliBeslutter__skal_kaste_exception_nar_veileder_er_ansvarlig() {
        Vedtak vedtak = new Vedtak();
        vedtak.setVeilederIdent(TEST_VEILEDER_IDENT);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.bliBeslutter(TEST_FNR));
    }

    @Test
    public void bliBeslutter__skal_ikke_endres_hvis_allerede_beslutter() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_VEILEDER_IDENT);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () ->  beslutterService.bliBeslutter(TEST_FNR));
    }

    @Test
    public void bliBeslutter__skal_opprette_system_melding_bli_beslutter() {
        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        beslutterService.bliBeslutter(TEST_FNR);

        verify(meldingRepository, times(1))
                .opprettSystemMelding(anyLong(), eq(SystemMeldingType.BLITT_BESLUTTER), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void bliBeslutter__skal_opprette_system_melding_overta_for_beslutter() {
        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        vedtak.setBeslutterIdent("TIDLIGERE BESLUTTER IDENT");

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        beslutterService.bliBeslutter(TEST_FNR);

        verify(meldingRepository, times(1))
                .opprettSystemMelding(anyLong(), eq(SystemMeldingType.TATT_OVER_SOM_BESLUTTER), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void godkjennVedtak__skal_sette_godkjent() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_VEILEDER_IDENT);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        beslutterService.setGodkjentAvBeslutter(TEST_FNR);

        verify(vedtaksstotteRepository, times(1)).setBeslutterProsessStatus(anyLong(), eq(GODKJENT_AV_BESLUTTER));
    }

    @Test
    public void godkjennVedtak__skal_kaste_exception_hvis_ikke_beslutter() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_BESLUTTER_IDENT);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.setGodkjentAvBeslutter(TEST_FNR));
    }

    @Test
    public void godkjennVedtak__skal_opprette_system_melding() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_VEILEDER_IDENT);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        beslutterService.setGodkjentAvBeslutter(TEST_FNR);

        verify(meldingRepository, times(1))
                .opprettSystemMelding(anyLong(), eq(SystemMeldingType.BESLUTTER_HAR_GODKJENT), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void oppdaterBeslutterProsessStatus__skal_sette_riktig_status_for_veileder() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_BESLUTTER_IDENT);
        vedtak.setVeilederIdent(TEST_VEILEDER_IDENT);
        vedtak.setBeslutterProsessStatus(KLAR_TIL_VEILEDER);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        beslutterService.oppdaterBeslutterProsessStatus(TEST_FNR);

        verify(vedtaksstotteRepository, times(1)).setBeslutterProsessStatus(anyLong(), eq(KLAR_TIL_BESLUTTER));
    }

    @Test
    public void oppdaterBeslutterProsessStatus__skal_sette_riktig_status_for_beslutter() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_BESLUTTER_IDENT);
        vedtak.setVeilederIdent(TEST_VEILEDER_IDENT);
        vedtak.setBeslutterProsessStatus(KLAR_TIL_BESLUTTER);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_BESLUTTER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        beslutterService.oppdaterBeslutterProsessStatus(TEST_FNR);

        verify(vedtaksstotteRepository, times(1)).setBeslutterProsessStatus(anyLong(), eq(KLAR_TIL_VEILEDER));
    }

    @Test
    public void oppdaterBeslutterProsessStatus__skal_feile_hvis_status_er_lik() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_BESLUTTER_IDENT);
        vedtak.setBeslutterProsessStatus(KLAR_TIL_VEILEDER);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_BESLUTTER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.oppdaterBeslutterProsessStatus(TEST_FNR));
    }

    @Test
    public void oppdaterBeslutterProsessStatus__skal_feile_hvis_bruker_ikke_er_beslutter_eller_veileder() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_BESLUTTER_IDENT);
        vedtak.setVeilederIdent(TEST_VEILEDER_IDENT);
        vedtak.setBeslutterProsessStatus(KLAR_TIL_VEILEDER);

        when(authService.sjekkTilgangTilFnr(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_IKKE_ANSVARLIG_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.oppdaterBeslutterProsessStatus(TEST_FNR));
    }
}

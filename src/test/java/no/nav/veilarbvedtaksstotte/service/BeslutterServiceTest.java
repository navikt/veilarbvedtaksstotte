package no.nav.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.feil.UgyldigRequest;
import no.nav.veilarbvedtaksstotte.client.PersonClient;
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.veilarbvedtaksstotte.domain.PersonNavn;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.Veileder;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutterProsessStatus;
import no.nav.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.junit.Before;
import org.junit.Test;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class BeslutterServiceTest {

    private VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);

    private VedtakStatusEndringService vedtakStatusEndringService = mock(VedtakStatusEndringService.class);

    private BeslutteroversiktRepository beslutteroversiktRepository = mock(BeslutteroversiktRepository.class);

    private VeilederService veilederService = mock(VeilederService.class);

    private PersonClient personClient = mock(PersonClient.class);

    private AuthService authService = mock(AuthService.class);

    private BeslutterService beslutterService = new BeslutterService(
            authService, vedtaksstotteRepository, vedtakStatusEndringService,
            beslutteroversiktRepository, veilederService, personClient
    );

    @Before
    public void setup() {
        doReturn(TEST_VEILEDER_IDENT).when(authService).getInnloggetVeilederIdent();
        doReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID)).when(authService).sjekkTilgang(anyString());
        doReturn(TEST_FNR).when(authService).getFnrOrThrow(TEST_AKTOR_ID);

        when(veilederService.hentVeileder(TEST_VEILEDER_IDENT)).thenReturn(new Veileder().setIdent(TEST_VEILEDER_IDENT).setNavn("VEILEDER"));
        when(veilederService.hentEnhetNavn(anyString())).thenReturn(TEST_OPPFOLGINGSENHET_NAVN);
        when(personClient.hentPersonNavn(TEST_FNR)).thenReturn(new PersonNavn().setFornavn("FORNAVN").setEtternavn("ETTERNAVN"));
    }

    @Test
    public void startBeslutterProsess_skal_starte_beslutter_prosess_hvis_ikke_startet() {
        Vedtak vedtak = new Vedtak()
                .setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS)
                .setAktorId(TEST_AKTOR_ID)
                .setOppfolgingsenhetId(TEST_OPPFOLGINGSENHET_ID)
                .setVeilederIdent(TEST_VEILEDER_IDENT);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        beslutterService.startBeslutterProsess(TEST_FNR);

        verify(vedtaksstotteRepository, atLeastOnce()).setBeslutterProsessStartet(anyLong());
    }

    @Test
    public void startBeslutterProsess_skal_ikke_starte_beslutter_prosess_hvis_allerede_startet() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterProsessStartet(true);
        vedtak.setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        assertThrows(UgyldigRequest.class, () ->  beslutterService.startBeslutterProsess(TEST_FNR));
    }

    @Test
    public void startBeslutterProsess_skal_kaste_exception_hvis_feil_innsatsgruppe() {
        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        assertThrows(UgyldigRequest.class, () -> beslutterService.startBeslutterProsess(TEST_FNR));
    }

    @Test
    public void bliBeslutter_skal_ta_over_som_beslutter() {
        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        beslutterService.bliBeslutter(TEST_FNR);

        verify(vedtaksstotteRepository, atLeastOnce()).setBeslutter(anyLong(), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void bliBeslutter_skal_kaste_exception_nar_veileder_er_ansvarlig() {
        Vedtak vedtak = new Vedtak();
        vedtak.setVeilederIdent(TEST_VEILEDER_IDENT);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        assertThrows(IngenTilgang.class, () -> beslutterService.bliBeslutter(TEST_FNR));
    }

    @Test
    public void bliBeslutter_skal_ikke_endres_hvis_allerede_beslutter() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_VEILEDER_IDENT);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        assertThrows(UgyldigRequest.class, () ->  beslutterService.bliBeslutter(TEST_FNR));
    }

    @Test
    public void godkjennVedtak_skal_sette_godkjent() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_VEILEDER_IDENT);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        beslutterService.setGodkjentAvBeslutter(TEST_FNR);

        verify(vedtaksstotteRepository, atLeastOnce()).setGodkjentAvBeslutter(anyLong(), eq(true));
    }

    @Test
    public void godkjennVedtak_skal_kaste_exception_hvis_ikke_beslutter() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_BESLUTTER_IDENT);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        assertThrows(IngenTilgang.class, () -> beslutterService.setGodkjentAvBeslutter(TEST_FNR));
    }

    @Test
    public void oppdaterBeslutterProsessStatus_skal_sette_riktig_status_for_veileder() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_BESLUTTER_IDENT);
        vedtak.setVeilederIdent(TEST_VEILEDER_IDENT);
        vedtak.setBeslutterProsessStatus(BeslutterProsessStatus.KLAR_TIL_VEILEDER);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        beslutterService.oppdaterBeslutterProsessStatus(TEST_FNR);

        verify(vedtaksstotteRepository, atLeastOnce()).setBeslutterProsessStatus(anyLong(), eq(BeslutterProsessStatus.KLAR_TIL_BESLUTTER));
    }

    @Test
    public void oppdaterBeslutterProsessStatus_skal_sette_riktig_status_for_beslutter() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_BESLUTTER_IDENT);
        vedtak.setVeilederIdent(TEST_VEILEDER_IDENT);
        vedtak.setBeslutterProsessStatus(BeslutterProsessStatus.KLAR_TIL_BESLUTTER);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_BESLUTTER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        beslutterService.oppdaterBeslutterProsessStatus(TEST_FNR);

        verify(vedtaksstotteRepository, atLeastOnce()).setBeslutterProsessStatus(anyLong(), eq(BeslutterProsessStatus.KLAR_TIL_VEILEDER));
    }

    @Test
    public void oppdaterBeslutterProsessStatus_skal_feile_hvis_status_er_lik() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_BESLUTTER_IDENT);
        vedtak.setBeslutterProsessStatus(BeslutterProsessStatus.KLAR_TIL_VEILEDER);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_BESLUTTER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        assertThrows(Feil.class, () -> beslutterService.oppdaterBeslutterProsessStatus(TEST_FNR));
    }

    @Test
    public void oppdaterBeslutterProsessStatus_skal_feile_hvis_bruker_ikke_er_beslutter_eller_veileder() {
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_BESLUTTER_IDENT);
        vedtak.setVeilederIdent(TEST_VEILEDER_IDENT);
        vedtak.setBeslutterProsessStatus(BeslutterProsessStatus.KLAR_TIL_VEILEDER);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_IKKE_ANSVARLIG_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        assertThrows(IngenTilgang.class, () -> beslutterService.oppdaterBeslutterProsessStatus(TEST_FNR));
    }
}

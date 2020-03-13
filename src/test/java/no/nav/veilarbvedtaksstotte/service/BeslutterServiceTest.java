package no.nav.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.feil.UgyldigRequest;
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.junit.Test;


import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class BeslutterServiceTest {

    @Test
    public void startBeslutterProsess_skal_starte_beslutter_prosess_hvis_ikke_startet() {
        AuthService authService = mock(AuthService.class);
        VedtaksstotteRepository repository = mock(VedtaksstotteRepository.class);
        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(repository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        BeslutterService beslutterService = new BeslutterService(authService, repository);

        beslutterService.startBeslutterProsess(TEST_FNR);

        verify(repository, atLeastOnce()).setBeslutterProsessStartet(anyLong());
    }

    @Test
    public void startBeslutterProsess_skal_ikke_starte_beslutter_prosess_hvis_allerede_startet() {
        AuthService authService = mock(AuthService.class);
        VedtaksstotteRepository repository = mock(VedtaksstotteRepository.class);
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterProsessStartet(true);
        vedtak.setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(repository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        BeslutterService beslutterService = new BeslutterService(authService, repository);

        assertThrows(UgyldigRequest.class, () ->  beslutterService.startBeslutterProsess(TEST_FNR));
    }

    @Test
    public void startBeslutterProsess_skal_kaste_exception_hvis_feil_innsatsgruppe() {
        AuthService authService = mock(AuthService.class);
        VedtaksstotteRepository repository = mock(VedtaksstotteRepository.class);
        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(repository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        BeslutterService beslutterService = new BeslutterService(authService, repository);

        assertThrows(UgyldigRequest.class, () -> beslutterService.startBeslutterProsess(TEST_FNR));
    }

    @Test
    public void bliBeslutter_skal_ta_over_som_beslutter() {
        AuthService authService = mock(AuthService.class);
        VedtaksstotteRepository repository = mock(VedtaksstotteRepository.class);
        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(repository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        BeslutterService beslutterService = new BeslutterService(authService, repository);

        beslutterService.bliBeslutter(TEST_FNR);

        verify(repository, atLeastOnce()).setBeslutter(anyLong(), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void bliBeslutter_skal_kaste_exception_nar_veileder_er_ansvarlig() {
        AuthService authService = mock(AuthService.class);
        VedtaksstotteRepository repository = mock(VedtaksstotteRepository.class);
        Vedtak vedtak = new Vedtak();
        vedtak.setVeilederIdent(TEST_VEILEDER_IDENT);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(repository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        BeslutterService beslutterService = new BeslutterService(authService, repository);

        assertThrows(IngenTilgang.class, () -> beslutterService.bliBeslutter(TEST_FNR));
    }

    @Test
    public void bliBeslutter_skal_ikke_endres_hvis_allerede_beslutter() {
        AuthService authService = mock(AuthService.class);
        VedtaksstotteRepository repository = mock(VedtaksstotteRepository.class);
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_VEILEDER_IDENT);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(repository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        BeslutterService beslutterService = new BeslutterService(authService, repository);

        assertThrows(UgyldigRequest.class, () ->  beslutterService.bliBeslutter(TEST_FNR));
    }

    @Test
    public void godkjennVedtak_skal_sette_godkjent() {
        AuthService authService = mock(AuthService.class);
        VedtaksstotteRepository repository = mock(VedtaksstotteRepository.class);
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_VEILEDER_IDENT);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(repository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        BeslutterService beslutterService = new BeslutterService(authService, repository);

        beslutterService.setGodkjentAvBeslutter(TEST_FNR);

        verify(repository, atLeastOnce()).setGodkjentAvBeslutter(anyLong(), eq(true));
    }

    @Test
    public void godkjennVedtak_skal_kaste_exception_hvis_ikke_beslutter() {
        AuthService authService = mock(AuthService.class);
        VedtaksstotteRepository repository = mock(VedtaksstotteRepository.class);
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterIdent(TEST_BESLUTTER_IDENT);
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(repository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        BeslutterService beslutterService = new BeslutterService(authService, repository);

        assertThrows(IngenTilgang.class, () -> beslutterService.setGodkjentAvBeslutter(TEST_FNR));
    }

}

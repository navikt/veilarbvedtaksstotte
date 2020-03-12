package no.nav.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.UgyldigRequest;
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;


import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class BeslutterServiceTest {

    @Test
    public void skal_starte_beslutter_prosess_hvis_ikke_startet() {
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
    public void skal_ikke_starte_beslutter_prosess_hvis_allerede_startet() {
        AuthService authService = mock(AuthService.class);
        VedtaksstotteRepository repository = mock(VedtaksstotteRepository.class);
        Vedtak vedtak = new Vedtak();
        vedtak.setBeslutterProsessStartet(true);
        vedtak.setInnsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(repository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        BeslutterService beslutterService = new BeslutterService(authService, repository);

        beslutterService.startBeslutterProsess(TEST_FNR);

        verify(repository, never()).setBeslutterProsessStartet(anyLong());
    }

    @Test
    public void skal_kaste_exception_for_start_beslutter_prosess_hvis_feil_innsatsgruppe() {
        AuthService authService = mock(AuthService.class);
        VedtaksstotteRepository repository = mock(VedtaksstotteRepository.class);
        Vedtak vedtak = new Vedtak();
        vedtak.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(repository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(vedtak);

        BeslutterService beslutterService = new BeslutterService(authService, repository);

        assertThrows(UgyldigRequest.class, () -> beslutterService.startBeslutterProsess(TEST_FNR));
    }

}

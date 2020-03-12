package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.junit.Test;


import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_AKTOR_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static org.mockito.Mockito.*;

public class BeslutterServiceTest {

    @Test
    public void skal_starte_beslutter_prosess_hvis_ikke_startet() {
        AuthService authService = mock(AuthService.class);
        VedtaksstotteRepository repository = mock(VedtaksstotteRepository.class);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(repository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(new Vedtak());

        BeslutterService beslutterService = new BeslutterService(authService, repository);

        beslutterService.startBeslutterProsess(TEST_FNR);

        verify(repository, atLeastOnce()).setBeslutterProsessStartet(anyLong());
    }

    @Test
    public void skal_ikke_starte_beslutter_prosess_hvis_allerede_startet() {
        AuthService authService = mock(AuthService.class);
        VedtaksstotteRepository repository = mock(VedtaksstotteRepository.class);

        when(authService.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(repository.hentUtkastEllerFeil(TEST_AKTOR_ID)).thenReturn(new Vedtak().setBeslutterProsessStartet(true));

        BeslutterService beslutterService = new BeslutterService(authService, repository);

        beslutterService.startBeslutterProsess(TEST_FNR);

        verify(repository, never()).setBeslutterProsessStartet(anyLong());
    }

}

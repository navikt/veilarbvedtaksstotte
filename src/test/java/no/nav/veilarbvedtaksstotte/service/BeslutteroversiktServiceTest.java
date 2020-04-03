package no.nav.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.veilarbvedtaksstotte.client.VeiledereOgEnhetClient;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktSokFilter;
import no.nav.veilarbvedtaksstotte.domain.PortefoljeEnhet;
import no.nav.veilarbvedtaksstotte.domain.VeilederEnheterDTO;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BeslutteroversiktServiceTest {

    private final BeslutteroversiktRepository beslutteroversiktRepository = mock(BeslutteroversiktRepository.class);

    private final VeiledereOgEnhetClient veiledereOgEnhetClient = mock(VeiledereOgEnhetClient.class);

    private final BeslutteroversiktService beslutteroversiktService = new BeslutteroversiktService(beslutteroversiktRepository, veiledereOgEnhetClient);

    @Test
    public void sokEtterBruker__skal_sette_alle_veileder_enheter_hvis_enhet_filter_mangler() {
        when(beslutteroversiktRepository.sokEtterBrukere(any(), anyString())).thenReturn(Collections.emptyList());
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of(
                        new PortefoljeEnhet("1234", "test1"),
                        new PortefoljeEnhet("4321", "test1")
                ))
        );

        BeslutteroversiktSok sok = new BeslutteroversiktSok();

        beslutteroversiktService.sokEtterBruker(sok);
        ArgumentCaptor<BeslutteroversiktSok> argument1 = ArgumentCaptor.forClass(BeslutteroversiktSok.class);
        ArgumentCaptor<String> argument2 = ArgumentCaptor.forClass(String.class);
        verify(beslutteroversiktRepository).sokEtterBrukere(argument1.capture(), argument2.capture());

        assertEquals(2, argument1.getValue().getFilter().getEnheter().size());
    }

    @Test
    public void sokEtterBruker__skal_feile_hvis_ikke_tilgang_til_enhet() {
        when(beslutteroversiktRepository.sokEtterBrukere(any(), anyString())).thenReturn(Collections.emptyList());
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of(
                        new PortefoljeEnhet("1234", "test1"),
                        new PortefoljeEnhet("4321", "test1")
                ))
        );

        BeslutteroversiktSok sok = new BeslutteroversiktSok();
        sok.setFilter(new BeslutteroversiktSokFilter().setEnheter(List.of("5431")));

        assertThrows(IngenTilgang.class, () -> {
            beslutteroversiktService.sokEtterBruker(sok);
        });
    }

    @Test
    public void sokEtterBruker__skal_ikke_feile_hvis_tilgang_til_enheter() {
        when(beslutteroversiktRepository.sokEtterBrukere(any(), anyString())).thenReturn(Collections.emptyList());
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of(
                        new PortefoljeEnhet("1234", "test1"),
                        new PortefoljeEnhet("4321", "test1")
                ))
        );

        BeslutteroversiktSok sok = new BeslutteroversiktSok();
        sok.setFilter(new BeslutteroversiktSokFilter().setEnheter(List.of("4321", "1234")));

        beslutteroversiktService.sokEtterBruker(sok);
        verify(beslutteroversiktRepository, atLeastOnce()).sokEtterBrukere(any(), anyString());
    }

}

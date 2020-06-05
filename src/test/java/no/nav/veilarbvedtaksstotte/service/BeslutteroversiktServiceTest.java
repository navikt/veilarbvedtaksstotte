package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.client.api.VeiledereOgEnhetClient;
import no.nav.veilarbvedtaksstotte.domain.*;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BeslutteroversiktServiceTest {

    private final BeslutteroversiktRepository beslutteroversiktRepository = mock(BeslutteroversiktRepository.class);

    private final VeiledereOgEnhetClient veiledereOgEnhetClient = mock(VeiledereOgEnhetClient.class);

    private final AuthService authService = mock(AuthService.class);

    private final BeslutteroversiktService beslutteroversiktService = new BeslutteroversiktService(beslutteroversiktRepository, veiledereOgEnhetClient, authService);

    private final static BrukereMedAntall INGEN_BRUKERE = new BrukereMedAntall(Collections.emptyList(), 0);

    @Test
    public void sokEtterBruker__skal_sette_alle_veileder_enheter_hvis_enhet_filter_mangler() {
        when(beslutteroversiktRepository.sokEtterBrukere(any(), anyString())).thenReturn(INGEN_BRUKERE);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, Arrays.asList(
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
        when(beslutteroversiktRepository.sokEtterBrukere(any(), anyString())).thenReturn(INGEN_BRUKERE);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, Arrays.asList(
                        new PortefoljeEnhet("1234", "test1"),
                        new PortefoljeEnhet("4321", "test1")
                ))
        );

        BeslutteroversiktSok sok = new BeslutteroversiktSok();
        sok.setFilter(new BeslutteroversiktSokFilter().setEnheter(Arrays.asList("5431")));

        assertThrows(ResponseStatusException.class, () -> {
            beslutteroversiktService.sokEtterBruker(sok);
        });
    }

    @Test
    public void sokEtterBruker__skal_ikke_feile_hvis_tilgang_til_enheter() {
        when(beslutteroversiktRepository.sokEtterBrukere(any(), anyString())).thenReturn(INGEN_BRUKERE);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, Arrays.asList(
                        new PortefoljeEnhet("1234", "test1"),
                        new PortefoljeEnhet("4321", "test1")
                ))
        );

        BeslutteroversiktSok sok = new BeslutteroversiktSok();
        sok.setFilter(new BeslutteroversiktSokFilter().setEnheter(Arrays.asList("4321", "1234")));

        beslutteroversiktService.sokEtterBruker(sok);
        verify(beslutteroversiktRepository, atLeastOnce()).sokEtterBrukere(any(), anyString());
    }

    @Test
    public void sokEtterBruker__skal_sensurere_brukere() {
        BeslutteroversiktService beslutteroversiktService =
                spy(new BeslutteroversiktService(beslutteroversiktRepository, veiledereOgEnhetClient, authService));

        when(beslutteroversiktRepository.sokEtterBrukere(any(), anyString())).thenReturn(INGEN_BRUKERE);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, Arrays.asList(
                        new PortefoljeEnhet("1234", "test1"),
                        new PortefoljeEnhet("4321", "test1")
                ))
        );

        beslutteroversiktService.sokEtterBruker(new BeslutteroversiktSok());
        verify(beslutteroversiktService).sensurerBrukere(anyList());
    }

    @Test
    public void sensurerBrukere__skal_fjerne_informasjon_pa_brukere_uten_tilgang() {
        String bruker1Fnr = "11111111111";
        String bruker2Fnr = "22222222222";
        Map<String, Boolean> brukerTilganger = new HashMap<>();
        brukerTilganger.put(bruker1Fnr, true);
        brukerTilganger.put(bruker2Fnr, false);

        when(authService.harInnloggetVeilederTilgangTilBrukere(any()))
                .thenReturn(brukerTilganger);

        BeslutteroversiktBruker bruker1 = new BeslutteroversiktBruker()
               .setBrukerFnr(bruker1Fnr)
               .setBrukerFornavn("Bruker 1")
               .setBrukerEtternavn("Bruker 1");

        BeslutteroversiktBruker bruker2 = new BeslutteroversiktBruker()
                .setBrukerFnr(bruker2Fnr)
                .setBrukerFornavn("Bruker 2")
                .setBrukerEtternavn("Bruker 2");

        List<BeslutteroversiktBruker> brukere = Arrays.asList(bruker1, bruker2);

        beslutteroversiktService.sensurerBrukere(brukere);

        assertEquals(bruker1.getBrukerFnr(), brukere.get(0).getBrukerFnr());
        assertEquals(bruker1.getBrukerFornavn(), brukere.get(0).getBrukerFornavn());
        assertEquals(bruker1.getBrukerEtternavn(), brukere.get(0).getBrukerEtternavn());

        assertEquals("", brukere.get(1).getBrukerFnr());
        assertEquals("", brukere.get(1).getBrukerFornavn());
        assertEquals("", brukere.get(1).getBrukerEtternavn());
    }

    @Test
    public void sensurerBrukere__skal_fjerne_informasjon_pa_bruker_som_det_mangler_sjekk_pa() {
        when(authService.harInnloggetVeilederTilgangTilBrukere(any()))
                .thenReturn(Collections.emptyMap());

        BeslutteroversiktBruker bruker1 = new BeslutteroversiktBruker()
                .setBrukerFnr("11111111111")
                .setBrukerFornavn("Bruker 1")
                .setBrukerEtternavn("Bruker 1");


        List<BeslutteroversiktBruker> brukere = Arrays.asList(bruker1);

        beslutteroversiktService.sensurerBrukere(brukere);

        assertEquals("", brukere.get(0).getBrukerFnr());
        assertEquals("", brukere.get(0).getBrukerFornavn());
        assertEquals("", brukere.get(0).getBrukerEtternavn());
    }

}

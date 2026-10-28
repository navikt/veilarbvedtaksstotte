package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.PortefoljeEnhet;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.VeilederEnheterDTO;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSokFilter;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BrukereMedAntall;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.EnhetMedNavn;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT_2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class BeslutteroversiktServiceTest {

    private final BeslutteroversiktRepository beslutteroversiktRepository = mock(BeslutteroversiktRepository.class);

    private final VeilarbveilederClient veiledereOgEnhetClient = mock(VeilarbveilederClient.class);

    private final AuthService authService = mock(AuthService.class);

    private final BeslutteroversiktService beslutteroversiktService = new BeslutteroversiktService(beslutteroversiktRepository, veiledereOgEnhetClient, authService);

    private final static BrukereMedAntall INGEN_BRUKERE = new BrukereMedAntall(Collections.emptyList(), 0);

    @Test
    public void hentEnheterMedUtkast__skjuler_kode6_enhet_uten_tilgang() {
        EnhetMedNavn vanligEnhet = new EnhetMedNavn("1234", "Nav Eksempel");
        EnhetMedNavn kode6Enhet = new EnhetMedNavn("2103", "Nav Vikafossen");
        when(beslutteroversiktRepository.hentEnheterMedUtkast()).thenReturn(List.of(vanligEnhet, kode6Enhet));
        when(authService.harTilgangTilKode6Brukere()).thenReturn(false);

        assertEquals(List.of(vanligEnhet), beslutteroversiktService.hentEnheterMedUtkast());
        verify(authService).harTilgangTilKode6Brukere();
    }

    @Test
    public void hentEnheterMedUtkast__viser_kode6_enhet_med_tilgang() {
        EnhetMedNavn kode6Enhet = new EnhetMedNavn("2103", "Nav Vikafossen");
        when(beslutteroversiktRepository.hentEnheterMedUtkast()).thenReturn(List.of(kode6Enhet));
        when(authService.harTilgangTilKode6Brukere()).thenReturn(true);

        assertEquals(List.of(kode6Enhet), beslutteroversiktService.hentEnheterMedUtkast());
        verify(authService).harTilgangTilKode6Brukere();
    }

    @Test
    public void sokEtterBruker__skal_sette_alle_veileder_enheter_hvis_enhet_filter_mangler() {
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(beslutteroversiktRepository.sokEtterBrukere(any(), any(NavIdent.class))).thenReturn(INGEN_BRUKERE);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT_2, Arrays.asList(
                        new PortefoljeEnhet("1234", "test1"),
                        new PortefoljeEnhet("4321", "test1")
                ))
        );

        BeslutteroversiktSok sok = new BeslutteroversiktSok();

        beslutteroversiktService.sokEtterBruker(sok);
        ArgumentCaptor<BeslutteroversiktSok> argument1 = ArgumentCaptor.forClass(BeslutteroversiktSok.class);
        ArgumentCaptor<NavIdent> argument2 = ArgumentCaptor.forClass(NavIdent.class);
        verify(beslutteroversiktRepository).sokEtterBrukere(argument1.capture(), argument2.capture());

        assertEquals(2, argument1.getValue().getFilter().getEnheter().size());
        assertEquals(TEST_VEILEDER_IDENT, argument2.getValue().get());
    }

    @Test
    public void sokEtterBruker__skal_feile_hvis_ikke_tilgang_til_enhet() {
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(beslutteroversiktRepository.sokEtterBrukere(any(), any(NavIdent.class))).thenReturn(INGEN_BRUKERE);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, Arrays.asList(
                        new PortefoljeEnhet("1234", "test1"),
                        new PortefoljeEnhet("4321", "test1")
                ))
        );

        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .withFilter(new BeslutteroversiktSokFilter().withEnheter(Arrays.asList("5431")));

        assertThrows(ResponseStatusException.class, () -> {
            beslutteroversiktService.sokEtterBruker(sok);
        });
    }

    @Test
    public void sokEtterBruker__skal_ikke_feile_hvis_tilgang_til_enheter() {
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(beslutteroversiktRepository.sokEtterBrukere(any(), any(NavIdent.class))).thenReturn(INGEN_BRUKERE);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, Arrays.asList(
                        new PortefoljeEnhet("1234", "test1"),
                        new PortefoljeEnhet("4321", "test1")
                ))
        );

        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .withFilter(new BeslutteroversiktSokFilter().withEnheter(Arrays.asList("4321", "1234")));

        beslutteroversiktService.sokEtterBruker(sok);
        verify(beslutteroversiktRepository, atLeastOnce()).sokEtterBrukere(any(), any(NavIdent.class));
    }

    @Test
    public void sokEtterBruker__skal_sensurere_brukere() {
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        BeslutteroversiktService beslutteroversiktService =
                spy(new BeslutteroversiktService(beslutteroversiktRepository, veiledereOgEnhetClient, authService));

        when(beslutteroversiktRepository.sokEtterBrukere(any(), any(NavIdent.class))).thenReturn(INGEN_BRUKERE);
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
    public void sokEtterBruker__skal_returnere_sensurerte_brukere() {
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of(new PortefoljeEnhet("1234", "test1")))
        );
        BeslutteroversiktBruker bruker = new BeslutteroversiktBruker()
                .withVedtakId(42)
                .withBrukerFnr("11111111111")
                .withBrukerFornavn("Fornavn")
                .withBrukerEtternavn("Etternavn");
        when(beslutteroversiktRepository.sokEtterBrukere(any(), any(NavIdent.class)))
                .thenReturn(new BrukereMedAntall(List.of(bruker), 1));
        when(authService.harInnloggetVeilederTilgangTilBrukere(any())).thenReturn(Map.of("11111111111", false));

        BrukereMedAntall resultat = beslutteroversiktService.sokEtterBruker(new BeslutteroversiktSok());

        assertEquals(1, resultat.getTotaltAntall());
        assertEquals(List.of(bruker.withBrukerFnr("").withBrukerFornavn("").withBrukerEtternavn("")), resultat.getBrukere());
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
                .withBrukerFnr(bruker1Fnr)
                .withBrukerFornavn("Bruker 1")
                .withBrukerEtternavn("Bruker 1");

        BeslutteroversiktBruker bruker2 = new BeslutteroversiktBruker()
                .withBrukerFnr(bruker2Fnr)
                .withBrukerFornavn("Bruker 2")
                .withBrukerEtternavn("Bruker 2");

        List<BeslutteroversiktBruker> brukere = Arrays.asList(bruker1, bruker2);

        List<BeslutteroversiktBruker> sensurert = beslutteroversiktService.sensurerBrukere(brukere);

        assertEquals(bruker1.getBrukerFnr(), sensurert.get(0).getBrukerFnr());
        assertEquals(bruker1.getBrukerFornavn(), sensurert.get(0).getBrukerFornavn());
        assertEquals(bruker1.getBrukerEtternavn(), sensurert.get(0).getBrukerEtternavn());

        assertEquals("", sensurert.get(1).getBrukerFnr());
        assertEquals("", sensurert.get(1).getBrukerFornavn());
        assertEquals("", sensurert.get(1).getBrukerEtternavn());
    }

    @Test
    public void sensurerBrukere__skal_fjerne_informasjon_pa_bruker_som_det_mangler_sjekk_pa() {
        when(authService.harInnloggetVeilederTilgangTilBrukere(any()))
                .thenReturn(Collections.emptyMap());

        BeslutteroversiktBruker bruker1 = new BeslutteroversiktBruker()
                .withBrukerFnr("11111111111")
                .withBrukerFornavn("Bruker 1")
                .withBrukerEtternavn("Bruker 1");


        List<BeslutteroversiktBruker> brukere = Arrays.asList(bruker1);

        List<BeslutteroversiktBruker> sensurert = beslutteroversiktService.sensurerBrukere(brukere);

        assertEquals("", sensurert.get(0).getBrukerFnr());
        assertEquals("", sensurert.get(0).getBrukerFornavn());
        assertEquals("", sensurert.get(0).getBrukerEtternavn());
    }

    @Test
    public void sokEtterBruker__skal_feile_for_sok_hvis_veileder_enhet_mangler_id() {
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of(new PortefoljeEnhet(null, "test1")))
        );

        assertThrows(IllegalStateException.class, () -> beslutteroversiktService.sokEtterBruker(new BeslutteroversiktSok()));
        verifyNoInteractions(beslutteroversiktRepository);
    }

    @Test
    public void sensurerBrukere__skal_feile_for_tilgangssjekk_hvis_bruker_mangler_fnr() {
        List<BeslutteroversiktBruker> brukere = List.of(new BeslutteroversiktBruker().withBrukerFornavn("Bruker 1"));

        assertThrows(IllegalStateException.class, () -> beslutteroversiktService.sensurerBrukere(brukere));
        verifyNoInteractions(authService);
    }

    @Test
    public void sokEtterBruker__skal_gi_403_hvis_sokt_enhet_er_null() {
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of(new PortefoljeEnhet("1234", "test1")))
        );

        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .withFilter(new BeslutteroversiktSokFilter().withEnheter(Arrays.asList("1234", null)));

        ResponseStatusException feil = assertThrows(ResponseStatusException.class, () -> beslutteroversiktService.sokEtterBruker(sok));
        assertEquals(HttpStatus.FORBIDDEN, feil.getStatusCode());
        verifyNoInteractions(beslutteroversiktRepository);
    }

}

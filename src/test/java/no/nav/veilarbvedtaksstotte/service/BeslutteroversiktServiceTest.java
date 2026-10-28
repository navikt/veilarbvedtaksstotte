package no.nav.veilarbvedtaksstotte.service;

import io.getunleash.DefaultUnleash;
import io.getunleash.UnleashContext;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.PortefoljeEnhet;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.VeilederEnheterDTO;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSokFilter;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktStatus;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BrukereMedAntall;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.EnhetMedNavn;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.nav.veilarbvedtaksstotte.utils.UnleashUtilsKt.MODIA_KVALITETSSIKRING_OPPFOLGINGSVEDTAK;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT_2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class BeslutteroversiktServiceTest {

    private final BeslutteroversiktRepository beslutteroversiktRepository = mock(BeslutteroversiktRepository.class);

    private final VeilarbveilederClient veiledereOgEnhetClient = mock(VeilarbveilederClient.class);

    private final AuthService authService = mock(AuthService.class);

    private final DefaultUnleash defaultUnleash = mock(DefaultUnleash.class);

    private final BeslutteroversiktService beslutteroversiktService = new BeslutteroversiktService(beslutteroversiktRepository, veiledereOgEnhetClient, authService, defaultUnleash);

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

        beslutteroversiktService.hentBeslutteroversikt(sok);
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
                .withFilter(new BeslutteroversiktSokFilter().withEnheter(List.of(EnhetId.of("5431"))));

        assertThrows(ResponseStatusException.class, () -> {
            beslutteroversiktService.hentBeslutteroversikt(sok);
        });
    }

    @Test
    public void sokEtterBruker__skal_gi_tomt_resultat_uten_a_soke_hvis_veileder_mangler_enheter() {
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of())
        );

        BrukereMedAntall resultat = beslutteroversiktService.hentBeslutteroversikt(new BeslutteroversiktSok());

        assertEquals(new BrukereMedAntall(List.of(), 0), resultat);
        verifyNoInteractions(beslutteroversiktRepository);
    }

    @Test
    public void sokEtterBruker__skal_gi_403_hvis_veileder_mangler_enheter_og_soker_pa_enhet() {
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of())
        );
        BeslutteroversiktSok sok = new BeslutteroversiktSok()
                .withFilter(new BeslutteroversiktSokFilter().withEnheter(List.of(EnhetId.of("1234"))));

        ResponseStatusException feil = assertThrows(ResponseStatusException.class,
                () -> beslutteroversiktService.hentBeslutteroversikt(sok));
        assertEquals(HttpStatus.FORBIDDEN, feil.getStatusCode());
        verifyNoInteractions(beslutteroversiktRepository);
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
                .withFilter(new BeslutteroversiktSokFilter().withEnheter(Arrays.asList(EnhetId.of("4321"), EnhetId.of("1234"))));

        beslutteroversiktService.hentBeslutteroversikt(sok);
        verify(beslutteroversiktRepository, atLeastOnce()).sokEtterBrukere(any(), any(NavIdent.class));
    }

    @Test
    public void sokEtterBruker__skal_sensurere_brukere() {
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        BeslutteroversiktService beslutteroversiktService =
                spy(new BeslutteroversiktService(beslutteroversiktRepository, veiledereOgEnhetClient, authService, defaultUnleash));

        when(beslutteroversiktRepository.sokEtterBrukere(any(), any(NavIdent.class))).thenReturn(INGEN_BRUKERE);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, Arrays.asList(
                        new PortefoljeEnhet("1234", "test1"),
                        new PortefoljeEnhet("4321", "test1")
                ))
        );

        beslutteroversiktService.hentBeslutteroversikt(new BeslutteroversiktSok());
        verify(beslutteroversiktService).sensurerBrukere(anyList());
    }

    @Test
    public void sokEtterBruker__skal_returnere_sensurerte_brukere() {
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of(new PortefoljeEnhet("1234", "test1")))
        );
        BeslutteroversiktBruker bruker = lagBruker(42, "11111111111", "Fornavn", "Etternavn");
        when(beslutteroversiktRepository.sokEtterBrukere(any(), any(NavIdent.class)))
                .thenReturn(new BrukereMedAntall(List.of(bruker), 1));
        when(authService.harInnloggetVeilederTilgangTilBrukere(any())).thenReturn(Map.of("11111111111", false));

        BrukereMedAntall resultat = beslutteroversiktService.hentBeslutteroversikt(new BeslutteroversiktSok());

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

        BeslutteroversiktBruker bruker1 = lagBruker(1, bruker1Fnr, "Bruker 1", "Bruker 1");
        BeslutteroversiktBruker bruker2 = lagBruker(2, bruker2Fnr, "Bruker 2", "Bruker 2");

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

        BeslutteroversiktBruker bruker1 = lagBruker(1, "11111111111", "Bruker 1", "Bruker 1");

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

        assertThrows(IllegalStateException.class, () -> beslutteroversiktService.hentBeslutteroversikt(new BeslutteroversiktSok()));
        verifyNoInteractions(beslutteroversiktRepository);
    }

    // --- Landsdekkende modus (togglen på). Testene feiler til stubbene merket 🔴 TODO er implementert. ---

    private static final List<EnhetId> VIKAFOSSEN = List.of(EnhetId.of("2103"));

    private void slaPaLandsdekkendeModus() {
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(defaultUnleash.isEnabled(eq(MODIA_KVALITETSSIKRING_OPPFOLGINGSVEDTAK), any(UnleashContext.class))).thenReturn(true);
        when(beslutteroversiktRepository.sokEtterBrukere(any(), any(NavIdent.class), anyList())).thenReturn(INGEN_BRUKERE);
    }

    private static BeslutteroversiktSok sokPaEnheter(String... enheter) {
        return new BeslutteroversiktSok().withFilter(
                new BeslutteroversiktSokFilter().withEnheter(Arrays.stream(enheter).map(EnhetId::of).toList()));
    }

    @Test
    public void landsdekkende__skal_evaluere_toggle_for_innlogget_veileder() {
        slaPaLandsdekkendeModus();

        beslutteroversiktService.hentBeslutteroversikt(new BeslutteroversiktSok());

        ArgumentCaptor<UnleashContext> context = ArgumentCaptor.forClass(UnleashContext.class);
        verify(defaultUnleash).isEnabled(eq(MODIA_KVALITETSSIKRING_OPPFOLGINGSVEDTAK), context.capture());
        assertEquals(TEST_VEILEDER_IDENT, context.getValue().getUserId().orElseThrow());
    }

    @Test
    public void landsdekkende__skal_gi_403_uten_modia_tilgang() {
        slaPaLandsdekkendeModus();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(authService).sjekkModiaGenerellTilgang();

        ResponseStatusException feil = assertThrows(ResponseStatusException.class,
                () -> beslutteroversiktService.hentBeslutteroversikt(new BeslutteroversiktSok()));

        assertEquals(HttpStatus.FORBIDDEN, feil.getStatusCode());
        verifyNoInteractions(beslutteroversiktRepository, veiledereOgEnhetClient);
    }

    @Test
    public void landsdekkende__skal_ekskludere_vikafossen_uten_kode6_tilgang() {
        slaPaLandsdekkendeModus();
        when(authService.harTilgangTilKode6Brukere()).thenReturn(false);
        BeslutteroversiktSok sok = new BeslutteroversiktSok();

        beslutteroversiktService.hentBeslutteroversikt(sok);

        verify(beslutteroversiktRepository).sokEtterBrukere(sok, NavIdent.of(TEST_VEILEDER_IDENT), VIKAFOSSEN);
        verifyNoInteractions(veiledereOgEnhetClient);
    }

    @Test
    public void landsdekkende__skal_ikke_ekskludere_enheter_med_kode6_tilgang() {
        slaPaLandsdekkendeModus();
        when(authService.harTilgangTilKode6Brukere()).thenReturn(true);
        BeslutteroversiktSok sok = new BeslutteroversiktSok();

        beslutteroversiktService.hentBeslutteroversikt(sok);

        verify(beslutteroversiktRepository).sokEtterBrukere(sok, NavIdent.of(TEST_VEILEDER_IDENT), List.of());
    }

    @Test
    public void landsdekkende__skal_gi_403_ved_sok_pa_vikafossen_uten_kode6_tilgang() {
        slaPaLandsdekkendeModus();
        when(authService.harTilgangTilKode6Brukere()).thenReturn(false);

        ResponseStatusException feil = assertThrows(ResponseStatusException.class,
                () -> beslutteroversiktService.hentBeslutteroversikt(sokPaEnheter("1234", "2103")));

        assertEquals(HttpStatus.FORBIDDEN, feil.getStatusCode());
        verifyNoInteractions(beslutteroversiktRepository);
    }

    @Test
    public void landsdekkende__skal_tillate_sok_pa_enhet_utenfor_veileders_enheter() {
        slaPaLandsdekkendeModus();
        when(authService.harTilgangTilKode6Brukere()).thenReturn(false);
        BeslutteroversiktSok sok = sokPaEnheter("9999");

        beslutteroversiktService.hentBeslutteroversikt(sok);

        verify(beslutteroversiktRepository).sokEtterBrukere(sok, NavIdent.of(TEST_VEILEDER_IDENT), VIKAFOSSEN);
        verifyNoInteractions(veiledereOgEnhetClient);
    }

    @Test
    public void landsdekkende__skal_sensurere_brukere() {
        slaPaLandsdekkendeModus();
        BeslutteroversiktBruker bruker = lagBruker(42, "11111111111", "Fornavn", "Etternavn");
        when(beslutteroversiktRepository.sokEtterBrukere(any(), any(NavIdent.class), anyList()))
                .thenReturn(new BrukereMedAntall(List.of(bruker), 1));
        when(authService.harInnloggetVeilederTilgangTilBrukere(any())).thenReturn(Map.of("11111111111", false));

        BrukereMedAntall resultat = beslutteroversiktService.hentBeslutteroversikt(new BeslutteroversiktSok());

        assertEquals(List.of(bruker.withBrukerFnr("").withBrukerFornavn("").withBrukerEtternavn("")), resultat.getBrukere());
    }

    private static BeslutteroversiktBruker lagBruker(long vedtakId, String fnr, String fornavn, String etternavn) {
        return new BeslutteroversiktBruker(
                vedtakId,
                fornavn,
                etternavn,
                "Nav Test",
                "1234",
                fnr,
                LocalDateTime.of(2026, 1, 1, 12, 0),
                BeslutteroversiktStatus.KLAR_TIL_BESLUTTER,
                LocalDateTime.of(2026, 1, 2, 12, 0),
                null,
                null,
                "Veileder"
        );
    }

}

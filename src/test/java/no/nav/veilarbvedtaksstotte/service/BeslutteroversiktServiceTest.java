package no.nav.veilarbvedtaksstotte.service;

import io.getunleash.DefaultUnleash;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.EnhetNavn;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.PortefoljeEnhet;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.VeilederEnheterDTO;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSokFilter;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BrukereMedAntall;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT;
import static no.nav.veilarbvedtaksstotte.utils.UnleashUtilsKt.TILGANGSMASKIN_INTEGRASJON;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

public class BeslutteroversiktServiceTest {

    private final BeslutteroversiktRepository beslutteroversiktRepository = mock(BeslutteroversiktRepository.class);

    private final VeilarbveilederClient veiledereOgEnhetClient = mock(VeilarbveilederClient.class);

    private final AuthService authService = mock(AuthService.class);
    private final DefaultUnleash unleashService = mock(DefaultUnleash.class);

    private final BeslutteroversiktService beslutteroversiktService = new BeslutteroversiktService(beslutteroversiktRepository, veiledereOgEnhetClient, authService, unleashService);

    private final static BrukereMedAntall INGEN_BRUKERE = new BrukereMedAntall(Collections.emptyList(), 0);

    @BeforeEach
    void aktiverNyTilgang() {
        when(unleashService.isEnabled(TILGANGSMASKIN_INTEGRASJON)).thenReturn(true);
    }

    @Test
    void sokEtterBruker__skal_kreve_enhetstilgang_i_dev_med_bryter_av() {
        when(unleashService.isEnabled(TILGANGSMASKIN_INTEGRASJON)).thenReturn(false);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter())
                .thenReturn(new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of(new PortefoljeEnhet("1234", "Nav To"))));
        var sok = new BeslutteroversiktSok()
                .setFilter(new BeslutteroversiktSokFilter().setEnheter(List.of("4321")));

        try (var miljo = mockStatic(EnvironmentUtils.class)) {
            miljo.when(EnvironmentUtils::isDevelopment).thenReturn(Optional.of(true));
            assertThrows(ResponseStatusException.class, () -> beslutteroversiktService.sokEtterBruker(sok));
        }
        verifyNoInteractions(beslutteroversiktRepository, authService);
    }

    @Test
    void sokEtterBruker__skal_bruke_gammel_persontilgang_uten_kode6_sjekk_med_bryter_av() {
        when(unleashService.isEnabled(TILGANGSMASKIN_INTEGRASJON)).thenReturn(false);
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter())
                .thenReturn(new VeilederEnheterDTO(TEST_VEILEDER_IDENT,
                        List.of(new PortefoljeEnhet("2103", "Nav Vikafossen"))));
        var bruker = new BeslutteroversiktBruker()
                .setBrukerFnr("11111111111").setBrukerFornavn("Fornavn").setBrukerEtternavn("Etternavn");
        when(beslutteroversiktRepository.sokEtterBrukere(any(), anyString()))
                .thenReturn(new BrukereMedAntall(List.of(bruker), 1));
        when(authService.harInnloggetVeilederTilgangTilBrukere(List.of("11111111111")))
                .thenReturn(Map.of("11111111111", false));

        try (var miljo = mockStatic(EnvironmentUtils.class)) {
            miljo.when(EnvironmentUtils::isDevelopment).thenReturn(Optional.of(true));
            beslutteroversiktService.sokEtterBruker(new BeslutteroversiktSok());
        }

        assertEquals("", bruker.getBrukerFnr());
        assertEquals("", bruker.getBrukerFornavn());
        assertEquals("", bruker.getBrukerEtternavn());
        verify(authService).harInnloggetVeilederTilgangTilBrukere(List.of("11111111111"));
        verify(authService, never()).harInnloggetVeilederTilgangTilBrukereMedKjerneregler(any());
        verify(authService, never()).harTilgangTilKode6Brukere();
    }

    @Test
    public void hentEnheterMedUtkast__skal_koble_navn_og_dele_enheter_etter_tilgang() {
        when(beslutteroversiktRepository.hentEnheterMedUtkast()).thenReturn(List.of("0001", "1234", "4321"));
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of(
                        new PortefoljeEnhet("1234", "Gammelt navn"),
                        new PortefoljeEnhet("9999", "Enhet uten utkast")
                ))
        );
        when(veiledereOgEnhetClient.hentEnhetNavn("0001")).thenReturn("Nav En");
        when(veiledereOgEnhetClient.hentEnhetNavn("1234")).thenReturn("Nav To");
        when(veiledereOgEnhetClient.hentEnhetNavn("4321")).thenReturn("Nav Tre");

        var enheter = beslutteroversiktService.hentEnheterMedUtkast();

        assertEquals(List.of(new EnhetNavn("1234", "Nav To")), enheter.medTilgang());
        assertEquals(List.of(new EnhetNavn("0001", "Nav En"), new EnhetNavn("4321", "Nav Tre")),
                enheter.utenTilgang());
        verify(veiledereOgEnhetClient, never()).hentEnhetNavn("9999");
        verify(authService, never()).harTilgangTilKode6Brukere();
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void hentEnheterMedUtkast__skal_bare_inkludere_vikafossen_med_kode6_tilgang(
            boolean harKode6Tilgang, boolean harEnhetTilgang) {
        when(beslutteroversiktRepository.hentEnheterMedUtkast()).thenReturn(List.of("1234", "2103"));
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, harEnhetTilgang
                        ? List.of(new PortefoljeEnhet("2103", "Nav Vikafossen"))
                        : List.of())
        );
        when(authService.harTilgangTilKode6Brukere()).thenReturn(harKode6Tilgang);
        when(veiledereOgEnhetClient.hentEnhetNavn("1234")).thenReturn("Nav To");
        when(veiledereOgEnhetClient.hentEnhetNavn("2103")).thenReturn("Nav Vikafossen");

        var enheter = beslutteroversiktService.hentEnheterMedUtkast();
        var vikafossen = new EnhetNavn("2103", "Nav Vikafossen");
        var annenEnhet = new EnhetNavn("1234", "Nav To");

        assertEquals(harKode6Tilgang && harEnhetTilgang ? List.of(vikafossen) : List.of(), enheter.medTilgang());
        assertEquals(harKode6Tilgang && !harEnhetTilgang ? List.of(annenEnhet, vikafossen) : List.of(annenEnhet),
                enheter.utenTilgang());
        verify(authService).harTilgangTilKode6Brukere();
        if (!harKode6Tilgang) {
            verify(veiledereOgEnhetClient, never()).hentEnhetNavn("2103");
        }
    }

    @Test
    void hentEnheterMedUtkast__skal_feile_ved_feil_i_kode6_tilgangssjekk() {
        when(beslutteroversiktRepository.hentEnheterMedUtkast()).thenReturn(List.of("2103"));
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of())
        );
        when(authService.harTilgangTilKode6Brukere()).thenThrow(new IllegalStateException("Tilgangssjekk feilet"));

        assertThrows(IllegalStateException.class, beslutteroversiktService::hentEnheterMedUtkast);
        verify(veiledereOgEnhetClient, never()).hentEnhetNavn("2103");
    }

    @Test
    public void hentEnheterMedUtkast__skal_returnere_tomme_lister_uten_utkast() {
        when(beslutteroversiktRepository.hentEnheterMedUtkast()).thenReturn(List.of());
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of(new PortefoljeEnhet("1234", "Nav To")))
        );

        var enheter = beslutteroversiktService.hentEnheterMedUtkast();

        assertEquals(List.of(), enheter.medTilgang());
        assertEquals(List.of(), enheter.utenTilgang());
        verify(veiledereOgEnhetClient, never()).hentEnhetNavn(anyString());
    }

    @Test
    public void hentEnheterMedUtkast__skal_returnere_alle_uten_tilgang_hvis_veileder_ikke_har_enheter() {
        when(beslutteroversiktRepository.hentEnheterMedUtkast()).thenReturn(List.of("1234"));
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of())
        );
        when(veiledereOgEnhetClient.hentEnhetNavn("1234")).thenReturn("Nav To");

        var enheter = beslutteroversiktService.hentEnheterMedUtkast();

        assertEquals(List.of(), enheter.medTilgang());
        assertEquals(List.of(new EnhetNavn("1234", "Nav To")), enheter.utenTilgang());
    }

    @Test
    public void hentEnheterMedUtkast__skal_feile_hvis_enhetsnavn_ikke_kan_hentes() {
        when(beslutteroversiktRepository.hentEnheterMedUtkast()).thenReturn(List.of("1234"));
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of())
        );
        when(veiledereOgEnhetClient.hentEnhetNavn("1234")).thenThrow(new IllegalStateException("Oppslag feilet"));

        assertThrows(IllegalStateException.class, beslutteroversiktService::hentEnheterMedUtkast);
    }

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

        try (var miljo = mockStatic(EnvironmentUtils.class)) {
            miljo.when(EnvironmentUtils::isDevelopment).thenReturn(Optional.of(false));
            assertThrows(ResponseStatusException.class, () -> beslutteroversiktService.sokEtterBruker(sok));
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    void sokEtterBruker__skal_bare_tillate_enheter_uten_enhetstilgang_i_dev(Boolean dev) {
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of())
        );
        var bruker = new BeslutteroversiktBruker()
                .setVedtakId(123L)
                .setBrukerOppfolgingsenhetId("1234")
                .setBrukerFnr("11111111111")
                .setBrukerFornavn("Fornavn")
                .setBrukerEtternavn("Etternavn");
        var sok = new BeslutteroversiktSok()
                .setFilter(new BeslutteroversiktSokFilter().setEnheter(List.of("1234")));
        when(beslutteroversiktRepository.sokEtterBrukere(same(sok), anyString()))
                .thenReturn(new BrukereMedAntall(List.of(bruker), 1));
        when(authService.harInnloggetVeilederTilgangTilBrukere(List.of("11111111111")))
                .thenReturn(Map.of("11111111111", false));

        try (var miljo = mockStatic(EnvironmentUtils.class)) {
            miljo.when(EnvironmentUtils::isDevelopment).thenReturn(Optional.ofNullable(dev));
            if (Boolean.TRUE.equals(dev)) {
                var resultat = beslutteroversiktService.sokEtterBruker(sok);
                assertEquals(1L, resultat.getTotaltAntall());
                assertEquals(123L, resultat.getBrukere().getFirst().getVedtakId());
                assertEquals("1234", resultat.getBrukere().getFirst().getBrukerOppfolgingsenhetId());
                assertEquals("", bruker.getBrukerFnr());
                assertEquals("", bruker.getBrukerFornavn());
                assertEquals("", bruker.getBrukerEtternavn());
                assertEquals(List.of("1234"), sok.getFilter().getEnheter());
            } else {
                assertThrows(ResponseStatusException.class, () -> beslutteroversiktService.sokEtterBruker(sok));
                verifyNoInteractions(beslutteroversiktRepository, authService);
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void sokEtterBruker__skal_kreve_kode6_tilgang_for_vikafossen_i_dev(
            boolean harKode6Tilgang, boolean brukStandardEnheter) {
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()).thenReturn(
                new VeilederEnheterDTO(TEST_VEILEDER_IDENT,
                        brukStandardEnheter ? List.of(new PortefoljeEnhet("2103", "Nav Vikafossen")) : List.of())
        );
        when(authService.harTilgangTilKode6Brukere()).thenReturn(harKode6Tilgang);
        when(beslutteroversiktRepository.sokEtterBrukere(any(), anyString())).thenReturn(INGEN_BRUKERE);
        var sok = new BeslutteroversiktSok();
        if (!brukStandardEnheter) {
            sok.setFilter(new BeslutteroversiktSokFilter().setEnheter(List.of("1234", "2103")));
        }

        try (var miljo = mockStatic(EnvironmentUtils.class)) {
            miljo.when(EnvironmentUtils::isDevelopment).thenReturn(Optional.of(true));
            if (harKode6Tilgang) {
                beslutteroversiktService.sokEtterBruker(sok);
                verify(beslutteroversiktRepository).sokEtterBrukere(same(sok), anyString());
            } else {
                var feil = assertThrows(ResponseStatusException.class,
                        () -> beslutteroversiktService.sokEtterBruker(sok));
                assertEquals(403, feil.getStatusCode().value());
                verifyNoInteractions(beslutteroversiktRepository);
            }
            verify(authService).harTilgangTilKode6Brukere();
        }
    }

    @Test
    void sokEtterBruker__skal_gi_tomt_resultat_uten_enheter_og_uten_enhetsfilter() {
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter())
                .thenReturn(new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of()));

        var resultat = beslutteroversiktService.sokEtterBruker(new BeslutteroversiktSok());

        assertEquals(List.of(), resultat.getBrukere());
        assertEquals(0L, resultat.getTotaltAntall());
        verifyNoInteractions(beslutteroversiktRepository, authService);
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
                spy(new BeslutteroversiktService(beslutteroversiktRepository, veiledereOgEnhetClient, authService, unleashService));

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

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    void sensurerBrukere__skal_bare_bruke_kjerneregler_i_dev(Boolean dev) {
        var synligBruker = new BeslutteroversiktBruker()
                .setBrukerFnr("11111111111").setBrukerFornavn("Fornavn").setBrukerEtternavn("Etternavn");
        var beskyttetBruker = new BeslutteroversiktBruker()
                .setBrukerFnr("22222222222").setBrukerFornavn("Beskyttet").setBrukerEtternavn("Person");
        var fnrs = List.of("11111111111", "22222222222");
        when(authService.harInnloggetVeilederTilgangTilBrukere(fnrs))
                .thenReturn(Map.of("11111111111", false, "22222222222", false));
        when(authService.harInnloggetVeilederTilgangTilBrukereMedKjerneregler(fnrs))
                .thenReturn(Map.of("11111111111", true, "22222222222", false));

        try (var miljo = mockStatic(EnvironmentUtils.class)) {
            miljo.when(EnvironmentUtils::isDevelopment).thenReturn(Optional.ofNullable(dev));
            beslutteroversiktService.sensurerBrukere(List.of(synligBruker, beskyttetBruker));
        }

        if (Boolean.TRUE.equals(dev)) {
            assertEquals("11111111111", synligBruker.getBrukerFnr());
            assertEquals("Fornavn", synligBruker.getBrukerFornavn());
            assertEquals("Etternavn", synligBruker.getBrukerEtternavn());
            verify(authService).harInnloggetVeilederTilgangTilBrukereMedKjerneregler(fnrs);
            verify(authService, never()).harInnloggetVeilederTilgangTilBrukere(any());
        } else {
            assertEquals("", synligBruker.getBrukerFnr());
            assertEquals("", synligBruker.getBrukerFornavn());
            assertEquals("", synligBruker.getBrukerEtternavn());
            verify(authService).harInnloggetVeilederTilgangTilBrukere(fnrs);
            verify(authService, never()).harInnloggetVeilederTilgangTilBrukereMedKjerneregler(any());
        }
        assertEquals("", beskyttetBruker.getBrukerFnr());
        assertEquals("", beskyttetBruker.getBrukerFornavn());
        assertEquals("", beskyttetBruker.getBrukerEtternavn());
    }

    @Test
    void sokEtterBruker__skal_vise_personopplysninger_uten_enhetstilgang_i_dev_ved_tilgang_med_kjerneregler() {
        when(veiledereOgEnhetClient.hentInnloggetVeilederEnheter())
                .thenReturn(new VeilederEnheterDTO(TEST_VEILEDER_IDENT, List.of()));
        var bruker = new BeslutteroversiktBruker()
                .setVedtakId(123L).setBrukerOppfolgingsenhetId("1234")
                .setBrukerFnr("11111111111").setBrukerFornavn("Fornavn").setBrukerEtternavn("Etternavn");
        var sok = new BeslutteroversiktSok()
                .setFilter(new BeslutteroversiktSokFilter().setEnheter(List.of("1234")));
        when(beslutteroversiktRepository.sokEtterBrukere(same(sok), anyString()))
                .thenReturn(new BrukereMedAntall(List.of(bruker), 1));
        when(authService.harInnloggetVeilederTilgangTilBrukereMedKjerneregler(List.of("11111111111")))
                .thenReturn(Map.of("11111111111", true));

        try (var miljo = mockStatic(EnvironmentUtils.class)) {
            miljo.when(EnvironmentUtils::isDevelopment).thenReturn(Optional.of(true));
            var resultat = beslutteroversiktService.sokEtterBruker(sok);
            assertEquals(1L, resultat.getTotaltAntall());
            assertEquals("11111111111", resultat.getBrukere().getFirst().getBrukerFnr());
            assertEquals("Fornavn", resultat.getBrukere().getFirst().getBrukerFornavn());
            assertEquals("Etternavn", resultat.getBrukere().getFirst().getBrukerEtternavn());
        }
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

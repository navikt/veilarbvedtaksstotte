package no.nav.veilarbvedtaksstotte.controller;

import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.EnhetNavn;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.EnheterMedUtkast;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BrukereMedAntall;
import no.nav.veilarbvedtaksstotte.service.AuthService;
import no.nav.veilarbvedtaksstotte.service.BeslutteroversiktService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BeslutteroversiktController.class)
class BeslutteroversiktControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BeslutteroversiktService beslutteroversiktService;

    @MockitoBean
    private AuthService authService;

    private MockedStatic<EnvironmentUtils> environmentUtils;

    @BeforeEach
    void settDevMiljo() {
        environmentUtils = mockStatic(EnvironmentUtils.class);
        environmentUtils.when(EnvironmentUtils::isDevelopment).thenReturn(Optional.of(true));
    }

    @AfterEach
    void ryddMiljo() {
        environmentUtils.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void hentEnheterMedUtkast__skal_avvise_utenfor_dev_og_ved_ukjent_miljo(boolean ukjentMiljo) throws Exception {
        environmentUtils.when(EnvironmentUtils::isDevelopment)
                .thenReturn(ukjentMiljo ? Optional.empty() : Optional.of(false));

        mockMvc.perform(get("/api/beslutteroversikt/enheter"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(authService, beslutteroversiktService);
    }

    @Test
    void sok__skal_fortsatt_vaere_tilgjengelig_utenfor_dev() throws Exception {
        environmentUtils.when(EnvironmentUtils::isDevelopment).thenReturn(Optional.of(false));
        when(beslutteroversiktService.sokEtterBruker(any())).thenReturn(new BrukereMedAntall(List.of(), 0));

        mockMvc.perform(post("/api/beslutteroversikt/sok")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void hentEnheterMedUtkast__skal_returnere_enhetsid_og_navn_delt_etter_tilgang() throws Exception {
        when(beslutteroversiktService.hentEnheterMedUtkast()).thenReturn(new EnheterMedUtkast(
                List.of(new EnhetNavn("1234", "Nav En")),
                List.of(new EnhetNavn("4321", "Nav To"))
        ));

        mockMvc.perform(get("/api/beslutteroversikt/enheter"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "medTilgang": [{"enhetId": "1234", "navn": "Nav En"}],
                          "utenTilgang": [{"enhetId": "4321", "navn": "Nav To"}]
                        }
                        """));

        var rekkefolge = inOrder(authService, beslutteroversiktService);
        rekkefolge.verify(authService).sjekkInternBruker();
        rekkefolge.verify(authService).sjekkTilgangTilModia();
        rekkefolge.verify(beslutteroversiktService).hentEnheterMedUtkast();
    }

    @Test
    void hentEnheterMedUtkast__skal_returnere_tomme_json_lister_uten_utkast() throws Exception {
        when(beslutteroversiktService.hentEnheterMedUtkast()).thenReturn(new EnheterMedUtkast(List.of(), List.of()));

        mockMvc.perform(get("/api/beslutteroversikt/enheter"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"medTilgang": [], "utenTilgang": []}
                        """));
    }

    @Test
    void sok__skal_sjekke_tilgang_for_sok() throws Exception {
        when(beslutteroversiktService.sokEtterBruker(any())).thenReturn(new BrukereMedAntall(List.of(), 0));

        mockMvc.perform(post("/api/beslutteroversikt/sok")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        var rekkefolge = inOrder(authService, beslutteroversiktService);
        rekkefolge.verify(authService).sjekkInternBruker();
        rekkefolge.verify(authService).sjekkTilgangTilModia();
        rekkefolge.verify(beslutteroversiktService).sokEtterBruker(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"enheter", "sok"})
    void skal_avvise_brukere_som_ikke_er_interne(String endepunkt) throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Ikke intern bruker"))
                .when(authService).sjekkInternBruker();

        mockMvc.perform(lagForesporsel(endepunkt)).andExpect(status().isForbidden());

        var rekkefolge = inOrder(authService);
        rekkefolge.verify(authService).sjekkInternBruker();
        verifyNoMoreInteractions(authService);
        verifyNoInteractions(beslutteroversiktService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"enheter", "sok"})
    void skal_avvise_brukere_uten_modia_tilgang(String endepunkt) throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(authService).sjekkTilgangTilModia();

        mockMvc.perform(lagForesporsel(endepunkt)).andExpect(status().isForbidden());

        var rekkefolge = inOrder(authService);
        rekkefolge.verify(authService).sjekkInternBruker();
        rekkefolge.verify(authService).sjekkTilgangTilModia();
        verifyNoInteractions(beslutteroversiktService);
    }

    private MockHttpServletRequestBuilder lagForesporsel(String endepunkt) {
        return endepunkt.equals("sok")
                ? post("/api/beslutteroversikt/sok").contentType(MediaType.APPLICATION_JSON).content("{}")
                : get("/api/beslutteroversikt/enheter");
    }
}

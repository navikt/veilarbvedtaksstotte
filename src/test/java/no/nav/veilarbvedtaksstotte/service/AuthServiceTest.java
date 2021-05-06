package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.abac.Pep;
import no.nav.common.abac.XacmlMapper;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Map;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthServiceTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    AuthContextHolder authContextHolder = AuthContextHolderThreadLocal.instance();
    AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);
    Pep pep = mock(Pep.class);
    VeilarbarenaClient arenaClient = mock(VeilarbarenaClient.class);
    UtrullingService utrullingService = mock(UtrullingService.class);
    AuthService authService = new AuthService(aktorOppslagClient, pep, arenaClient, null, null, authContextHolder, utrullingService);

    @Before
    public void setup() {
        when(aktorOppslagClient.hentAktorId(TEST_FNR)).thenReturn(AktorId.of(TEST_AKTOR_ID));
        when(aktorOppslagClient.hentFnr(AktorId.of(TEST_AKTOR_ID))).thenReturn(TEST_FNR);
        when(arenaClient.oppfolgingsenhet(TEST_FNR)).thenReturn(EnhetId.of(TEST_OPPFOLGINGSENHET_ID));
    }

    @Test
    public void sjekkTilgangTilBruker__gir_tilgang_for_intern_bruker() {
        when(pep.harVeilederTilgangTilPerson(any(), any(), any())).thenReturn(true);
        withContext(UserRole.INTERN, () -> {
            authService.sjekkTilgangTilBruker(TEST_FNR);
        });
    }

    @Test
    public void sjekkTilgangTilBruker__kaster_exception_for_andre_enn_ekstern_bruker() {
        when(pep.harVeilederTilgangTilPerson(any(), any(), any())).thenReturn(true);
        Arrays.stream(UserRole.values()).filter(userRole -> userRole != UserRole.INTERN).forEach(userRole -> {
            withContext(userRole, () -> {
                expectForbiddenException();
                authService.sjekkTilgangTilBruker(TEST_FNR);
            });
        });
    }

    @Test
    public void sjekkTilgangTilBruker__kaster_exception_ved_manglende_tilgang_til_bruker() {
        when(pep.harVeilederTilgangTilPerson(any(), any(), any())).thenReturn(false);
        withContext(UserRole.INTERN, () -> {
            expectForbiddenException();
            authService.sjekkTilgangTilBruker(TEST_FNR);
        });
    }

    @Test
    public void sjekkTilgangTilBrukerOgEnhet__sjekkTilgangTilBruker__gir_tilgang_for_intern_bruker() {
        when(pep.harVeilederTilgangTilPerson(any(), any(), any())).thenReturn(true);
        when(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(utrullingService.erUtrullet(EnhetId.of(TEST_OPPFOLGINGSENHET_ID))).thenReturn(true);
        withContext(UserRole.INTERN, () -> {
            authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR);
        });
    }

    @Test
    public void sjekkTilgangTilBrukerOgEnhet__kaster_exception_for_andre_enn_ekstern_bruker() {
        when(pep.harVeilederTilgangTilPerson(any(), any(), any())).thenReturn(true);
        when(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(utrullingService.erUtrullet(EnhetId.of(TEST_OPPFOLGINGSENHET_ID))).thenReturn(true);
        Arrays.stream(UserRole.values()).filter(userRole -> userRole != UserRole.INTERN).forEach(userRole -> {
            withContext(userRole, () -> {
                expectForbiddenException();
                authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR);
            });
        });
    }

    @Test
    public void sjekkTilgangTilBrukerOgEnhet__kaster_exception_ved_manglende_tilgang_til_bruker() {
        when(pep.harVeilederTilgangTilPerson(any(), any(), any())).thenReturn(false);
        when(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(utrullingService.erUtrullet(EnhetId.of(TEST_OPPFOLGINGSENHET_ID))).thenReturn(true);
        withContext(UserRole.INTERN, () -> {
            expectForbiddenException();
            authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR);
        });
    }

    @Test
    public void sjekkTilgangTilBrukerOgEnhet__kaster_exception_ved_manglende_tilgang_til_enhet() {
        when(pep.harVeilederTilgangTilPerson(any(), any(), any())).thenReturn(true);
        when(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(false);
        when(utrullingService.erUtrullet(EnhetId.of(TEST_OPPFOLGINGSENHET_ID))).thenReturn(true);
        withContext(UserRole.INTERN, () -> {
            expectForbiddenException();
            authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR);
        });
    }

    @Test
    public void sjekkTilgangTilBrukerOgEnhet__kaster_exception_dersom_enhet_ikke_er_utrullet() {
        when(pep.harVeilederTilgangTilPerson(any(), any(), any())).thenReturn(true);
        when(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(utrullingService.erUtrullet(EnhetId.of(TEST_OPPFOLGINGSENHET_ID))).thenReturn(false);
        withContext(UserRole.INTERN, () -> {
            expectForbiddenException();
            authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR);
        });
    }

    @Test
    public void lagSjekkTilgangRequest__skal_lage_riktig_request() {
        XacmlRequest request = authService.lagSjekkTilgangRequest("srvtest", "Z1234", Arrays.asList("11111111111", "2222222222"));

        String requestJson = XacmlMapper.mapRequestToEntity(request);
        String expectedRequestJson = TestUtils.readTestResourceFile("xacmlrequest-abac-tilgang.json");

        assertEquals(expectedRequestJson, requestJson);
    }

    @Test
    public void mapBrukerTilgangRespons__skal_mappe_riktig() {
        String responseJson = TestUtils.readTestResourceFile("xacmlresponse-abac-tilgang.json");
        XacmlResponse response = XacmlMapper.mapRawResponse(responseJson);

        Map<String, Boolean> tilgangTilBrukere = authService.mapBrukerTilgangRespons(response);

        assertTrue(tilgangTilBrukere.getOrDefault("11111111111", false));
        assertFalse(tilgangTilBrukere.getOrDefault("2222222222", false));
    }

    private void withContext(UserRole userRole, UnsafeRunnable runnable) {
        authContextHolder.withContext(AuthTestUtils.createAuthContext(userRole, TEST_VEILEDER_IDENT), runnable);
    }

    private void expectForbiddenException() {
        exceptionRule.expect(ResponseStatusException.class);
        exceptionRule.expectMessage(Matchers.startsWith("403 FORBIDDEN"));
    }
}

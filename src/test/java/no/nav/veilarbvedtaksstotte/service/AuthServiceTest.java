package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.abac.XacmlMapper;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.*;

public class AuthServiceTest {

    @Test
    public void lagSjekkTilgangRequest__skal_lage_riktig_request() {
        AuthService authService = new AuthService(null, null, null, null, null, null);

        XacmlRequest request = authService.lagSjekkTilgangRequest("srvtest", "Z1234", Arrays.asList("11111111111", "2222222222"));

        String requestJson = XacmlMapper.mapRequestToEntity(request);
        String expectedRequestJson = TestUtils.readTestResourceFile("xacmlrequest-abac-tilgang.json");

        assertEquals(expectedRequestJson, requestJson);
    }

    @Test
    public void mapBrukerTilgangRespons__skal_mappe_riktig() {
        AuthService authService = new AuthService(null, null, null, null, null, null);

        String responseJson = TestUtils.readTestResourceFile("xacmlresponse-abac-tilgang.json");
        XacmlResponse response = XacmlMapper.mapRawResponse(responseJson);

        Map<String, Boolean> tilgangTilBrukere = authService.mapBrukerTilgangRespons(response);

        assertTrue(tilgangTilBrukere.getOrDefault("11111111111", false));
        assertFalse(tilgangTilBrukere.getOrDefault("2222222222", false));
    }

}

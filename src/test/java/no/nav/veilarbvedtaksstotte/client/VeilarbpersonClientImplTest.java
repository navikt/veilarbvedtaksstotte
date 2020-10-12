package no.nav.veilarbvedtaksstotte.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class VeilarbpersonClientImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void skal_hente_cv_jobbprofil_json() {
        String cvJobbprofilJson = TestUtils.readTestResourceFile("cv-jobbprofil.json");
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbpersonClientImpl personClient = new VeilarbpersonClientImpl(apiUrl, () -> "TOKEN");

        givenThat(get("/api/person/cv_jobbprofil?fnr=1234")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Authorization", "Bearer TOKEN")
                        .withBody(cvJobbprofilJson))
        );

        String jsonResponse = personClient.hentCVOgJobbprofil("1234");

        assertEquals(cvJobbprofilJson, jsonResponse);
    }

    @Test
    public void skal_returnere_no_data_json_for_403_and_401() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbpersonClientImpl personClient = new VeilarbpersonClientImpl(apiUrl, () -> "TOKEN");

        String expectedJsonResponse = JsonUtils.createNoDataStr("Bruker har ikke delt CV/jobbprofil med NAV");

        givenThat(get("/api/person/cv_jobbprofil?fnr=1234").willReturn(aResponse().withStatus(401)));
        assertEquals(expectedJsonResponse, personClient.hentCVOgJobbprofil("1234"));

        givenThat(get("/api/person/cv_jobbprofil?fnr=1234").willReturn(aResponse().withStatus(403)));
        assertEquals(expectedJsonResponse, personClient.hentCVOgJobbprofil("1234"));
    }

    @Test
    public void skal_returnere_no_data_json_for_204_and_404() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbpersonClientImpl personClient = new VeilarbpersonClientImpl(apiUrl, () -> "TOKEN");

        String expectedJsonResponse = JsonUtils.createNoDataStr("Bruker har ikke fylt ut CV/jobbprofil");

        givenThat(get("/api/person/cv_jobbprofil?fnr=1234").willReturn(aResponse().withStatus(204)));
        assertEquals(expectedJsonResponse, personClient.hentCVOgJobbprofil("1234"));

        givenThat(get("/api/person/cv_jobbprofil?fnr=1234").willReturn(aResponse().withStatus(404)));
        assertEquals(expectedJsonResponse, personClient.hentCVOgJobbprofil("1234"));
    }

    @Test
    public void skal_sjekke_helse() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        VeilarbpersonClientImpl personClient = new VeilarbpersonClientImpl(apiUrl, () -> "TOKEN");

        givenThat(get("/internal/isAlive").willReturn(aResponse().withStatus(200)));

        personClient.checkHealth();
    }


}

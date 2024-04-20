package no.nav.veilarbvedtaksstotte.client.registrering;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WireMockTest
public class VeilarbregistreringClientTest {

    private static VeilarbregistreringClient veilarbregistreringClient;

    @BeforeAll
    public static void setup(WireMockRuntimeInfo wireMockRuntimeInfo) {
        String apiUrl = "http://localhost:" + wireMockRuntimeInfo.getHttpPort();
        veilarbregistreringClient = new VeilarbregistreringClientImpl(apiUrl, () -> "");
    }

    @Test
    public void test() {

        String response = TestUtils.readTestResourceFile("testdata/registrering.json");

        WireMock.givenThat(
                WireMock.post(WireMock.urlEqualTo("/api/v3/person/hent-registrering"))
                        .withRequestBody(WireMock.equalToJson("{\"fnr\":\"" + TEST_FNR.get() + "\"}"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withBody(response)
                        )
        );
        RegistreringData registreringData = veilarbregistreringClient.hentRegistreringData(TEST_FNR.get());

        assertEquals(registreringData, new RegistreringData(new RegistreringData.BrukerRegistrering(
                LocalDateTime.of(2021, 1, 18, 9, 48, 58).plus(762, MILLIS),
                new RegistreringData.Profilering(RegistreringData.ProfilertInnsatsgruppe.SITUASJONSBESTEMT_INNSATS))));
    }
}

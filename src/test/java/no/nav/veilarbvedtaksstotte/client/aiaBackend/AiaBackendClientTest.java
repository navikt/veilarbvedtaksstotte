package no.nav.veilarbvedtaksstotte.client.aiaBackend;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EgenvurderingResponseDTO;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@WireMockTest
public class AiaBackendClientTest {

    private static AiaBackendClient aiaBackendClient;

    @BeforeAll
    public static void setup(WireMockRuntimeInfo wireMockRuntimeInfo) {
        String apiUrl = "http://localhost:" + wireMockRuntimeInfo.getHttpPort();
        aiaBackendClient = new AiaBackendClientImpl(apiUrl, () -> "");
    }

    @Test
    void hentEgenvurdering_200_response() {

        String response = TestUtils.readTestResourceFile("testdata/egenvurdering-response.json");
        String forventetRequest =
                """
                            {
                               "foedselsnummer": "12345678912"
                            }
                        """;
        WireMock.givenThat(
                WireMock.post(WireMock.urlEqualTo("/veileder/behov-for-veiledning"))
                        .withRequestBody(WireMock.equalToJson(forventetRequest))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withBody(response)
                        )
        );
        EgenvurderingResponseDTO egenvurderingData = aiaBackendClient.hentEgenvurdering(new EgenvurderingForPersonDTO(TEST_FNR.get()));

        assertEquals(toJson(egenvurderingData), "{\"dato\":\"2024-04-10T11:07:55.337Z\",\"dialogId\":\"dialog-123\",\"oppfolging\":\"SITUASJONSBESTEMT_INNSATS\",\"tekster\":{\"sporsmal\":\"Testspm\",\"svar\":{\"STANDARD_INNSATS\":\"Svar jeg klarer meg\",\"SITUASJONSBESTEMT_INNSATS\":\"Svar jeg trenger hjelp\"}}}".trim());

    }

    @Test
    void hentEgenvurdering_204_response() {
        String forventetRequest =
                """
                            {
                               "foedselsnummer": "12345678912"
                            }
                        """;
        WireMock.givenThat(
                WireMock.post(WireMock.urlEqualTo("/veileder/behov-for-veiledning"))
                        .withRequestBody(WireMock.equalToJson(forventetRequest))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(204)
                        )
        );
        EgenvurderingResponseDTO egenvurderingData = aiaBackendClient.hentEgenvurdering(new EgenvurderingForPersonDTO(TEST_FNR.get()));

        assertNull(toJson(egenvurderingData));
    }
}

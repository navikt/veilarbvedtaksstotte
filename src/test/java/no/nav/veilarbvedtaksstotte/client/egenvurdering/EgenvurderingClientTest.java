package no.nav.veilarbvedtaksstotte.client.egenvurdering;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WireMockTest
public class EgenvurderingClientTest {

    private static EgenvurderingClient egenvurderingClient;
    @BeforeAll
    public static void setup(WireMockRuntimeInfo wireMockRuntimeInfo) {
        String apiUrl = "http://localhost:" + wireMockRuntimeInfo.getHttpPort();
        egenvurderingClient = new EgenvurderingClientImpl(apiUrl, () -> "");
    }

    @Test
    public void hentEgenvurdering_200_response() {

        String response = TestUtils.readTestResourceFile("egenvurdering-response.json");
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
        EgenvurderingResponseDTO egenvurderingData = egenvurderingClient.hentEgenvurdering(new EgenvurderingForPersonDTO(TEST_FNR.get()));

        assertEquals(toJson(egenvurderingData), "{\"dato\":\"2023-06-19T08:42:31.389Z\",\"dialogId\":\"dialog-123\",\"oppfolging\":\"SITUASJONSBESTEMT_INNSATS\",\"tekster\":{\"sporsmal\":\"Testspm\",\"svar\":{\"STANDARD_INNSATS\":\"Svar jeg klarer meg\",\"SITUASJONSBESTEMT_INNSATS\":\"Svar jeg trenger hjelp\"}}}".trim());

    }

    @Test
    public void hentEgenvurdering_204_response() {
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
        EgenvurderingResponseDTO egenvurderingData = egenvurderingClient.hentEgenvurdering(new EgenvurderingForPersonDTO(TEST_FNR.get()));

        assertEquals(toJson(egenvurderingData), null);
    }
}

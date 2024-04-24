package no.nav.veilarbvedtaksstotte.client.registrering;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import no.nav.veilarbvedtaksstotte.client.person.BehandlingsNummer;
import no.nav.veilarbvedtaksstotte.client.registrering.dto.BrukerRegistreringType;
import no.nav.veilarbvedtaksstotte.client.registrering.dto.RegistreringResponseDto;
import no.nav.veilarbvedtaksstotte.client.registrering.dto.RegistreringsdataDto;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static java.util.Collections.emptyList;
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
                        .withRequestBody(WireMock.equalToJson("{\"fnr\":\""+TEST_FNR+"\", \"behandlingsnummer\": \"" + BehandlingsNummer.VEDTAKSTOTTE.getValue() + "\"}"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withBody(response)
                        )
        );
        RegistreringResponseDto registreringData = veilarbregistreringClient.hentRegistreringData(TEST_FNR.get());

        assertEquals(registreringData, new RegistreringResponseDto(new RegistreringsdataDto(
                LocalDateTime.parse("2021-01-18T09:48:58.762"),
                null,
                emptyList(),
                null,
                new RegistreringsdataDto.Profilering(RegistreringsdataDto.ProfilertInnsatsgruppe.SITUASJONSBESTEMT_INNSATS, null, null),
                null,
                null,
                null

        ), BrukerRegistreringType.ORDINAER));
    }
}

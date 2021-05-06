package no.nav.veilarbvedtaksstotte.client.veilarboppfolging;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpHeaders;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.time.temporal.ChronoUnit.MILLIS;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static org.junit.Assert.assertEquals;

public class VeilarboppfolgingClientImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    VeilarboppfolgingClientImpl veilarboppfolgingClient;

    @Before
    public void setup() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        veilarboppfolgingClient = new VeilarboppfolgingClientImpl(apiUrl, () -> "USER_TOKEN", () -> "SYSTEM_TOKEN");
    }

    @Test
    public void hentOppfolgingData__skal_lage_riktig_request_og_parse_response() {
        String response = TestUtils.readTestResourceFile("veilarboppfolging_hentOppfolgingData.json");

        givenThat(get(urlEqualTo("/api/person/" + TEST_FNR + "/oppfolgingsstatus"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer USER_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(response))
        );

        OppfolgingsstatusDTO expectedData = new OppfolgingsstatusDTO();
        expectedData.setServicegruppe("VURDU");

        assertEquals(expectedData, veilarboppfolgingClient.hentOppfolgingData(TEST_FNR.get()));
    }

    @Test
    public void hentOppfolgingsperioder__skal_lage_riktig_request_og_parse_response() {
        String response = TestUtils.readTestResourceFile("veilarboppfolging_hentOppfolgingsperioder.json");

        givenThat(get(urlEqualTo("/api/oppfolging/oppfolgingsperioder?fnr=" + TEST_FNR.get()))
                .withQueryParam("fnr", equalTo(TEST_FNR.get()))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer SYSTEM_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(response))
        );

        OppfolgingPeriodeDTO expectedPeriode1 = new OppfolgingPeriodeDTO();
        expectedPeriode1.setStartDato(ZonedDateTime.of(2021, 5, 4, 9, 48, 58, 0, ZoneId.of("+2")).plus(762, MILLIS));
        expectedPeriode1.setSluttDato(ZonedDateTime.of(2021, 6, 4, 9, 48, 58, 0, ZoneId.of("+2")).plus(762, MILLIS));

        OppfolgingPeriodeDTO expectedPeriode2 = new OppfolgingPeriodeDTO();
        expectedPeriode2.setStartDato(ZonedDateTime.of(2020, 5, 4, 9, 48, 58, 0, ZoneId.of("+2")).plus(762, MILLIS));
        expectedPeriode2.setSluttDato(ZonedDateTime.of(2020, 6, 4, 9, 48, 58, 0, ZoneId.of("+2")).plus(762, MILLIS));

        List<OppfolgingPeriodeDTO> oppfolgingsperioder = veilarboppfolgingClient.hentOppfolgingsperioder(TEST_FNR.get());

        assertEquals(2, oppfolgingsperioder.size());
        assertEquals(expectedPeriode1, oppfolgingsperioder.get(0));
        assertEquals(expectedPeriode2, oppfolgingsperioder.get(1));
    }

}

package no.nav.veilarbvedtaksstotte.client.veilarboppfolging;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.time.temporal.ChronoUnit.MILLIS;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WireMockTest
public class VeilarboppfolgingClientImplTest {

    private static VeilarboppfolgingClientImpl veilarboppfolgingClient;

    @BeforeAll
    public static void setup(WireMockRuntimeInfo wireMockRuntimeInfo) {
        String apiUrl = "http://localhost:" + wireMockRuntimeInfo.getHttpPort();
        veilarboppfolgingClient = new VeilarboppfolgingClientImpl(apiUrl, () -> "SYSTEM_TOKEN");
    }

    @Test
    public void hentOppfolgingsperioder__skal_lage_riktig_request_og_parse_response() {
        String response = TestUtils.readTestResourceFile("veilarboppfolging_hentOppfolgingsperioder.json");

        givenThat(get(urlEqualTo("/api/v2/oppfolging/perioder?fnr=" + TEST_FNR.get()))
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

        List<OppfolgingPeriodeDTO> oppfolgingsperioder = veilarboppfolgingClient.hentOppfolgingsperioder(TEST_FNR);

        assertEquals(2, oppfolgingsperioder.size());
        assertEquals(expectedPeriode1, oppfolgingsperioder.get(0));
        assertEquals(expectedPeriode2, oppfolgingsperioder.get(1));
    }

    @Test
    public void hentGjeldendeOppfolgingsperiode__skal_lage_riktig_request_og_parse_response() {
        String response = TestUtils.readTestResourceFile("veilarboppfolging_hentGjeldendeOppfolgingsperiode.json");

        givenThat(get(urlEqualTo("/api/v2/oppfolging/periode/gjeldende?fnr=" + TEST_FNR.get()))
                .withQueryParam("fnr", equalTo(TEST_FNR.get()))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer SYSTEM_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(response))
        );

        OppfolgingPeriodeDTO expectedPeriode = new OppfolgingPeriodeDTO();
        expectedPeriode.setStartDato(ZonedDateTime.of(2021, 5, 4, 9, 48, 58, 0, ZoneId.of("+2")).plus(762, MILLIS));
        expectedPeriode.setSluttDato(ZonedDateTime.of(2021, 6, 4, 9, 48, 58, 0, ZoneId.of("+2")).plus(762, MILLIS));

        Optional<OppfolgingPeriodeDTO> gjeldendeOppfolgingsperiode = veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(TEST_FNR);

        assertEquals(Optional.of(expectedPeriode), gjeldendeOppfolgingsperiode);
    }

    @Test
    public void hentGjeldendeOppfolgingsperiode__skal_håndtere_manglende_respons() {

        givenThat(get(urlEqualTo("/api/v2/oppfolging/periode/gjeldende?fnr=" + TEST_FNR.get()))
                .withQueryParam("fnr", equalTo(TEST_FNR.get()))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer SYSTEM_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(204))
        );

        Optional<OppfolgingPeriodeDTO> gjeldendeOppfolgingsperiode =
                veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(TEST_FNR);

        assertEquals(Optional.empty(), gjeldendeOppfolgingsperiode);
    }
}

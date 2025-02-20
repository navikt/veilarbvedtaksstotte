package no.nav.veilarbvedtaksstotte.client.veilarboppfolging;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.SakDTO;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.time.temporal.ChronoUnit.MILLIS;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        String response = TestUtils.readTestResourceFile("testdata/veilarboppfolging_hentOppfolgingsperioder.json");

        givenThat(post(urlEqualTo("/api/v3/oppfolging/hent-perioder"))
                .withRequestBody(WireMock.equalToJson("{\"fnr\":\"" + TEST_FNR.get() + "\"}"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer SYSTEM_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(response))
        );

        OppfolgingPeriodeDTO expectedPeriode1 = new OppfolgingPeriodeDTO();
        expectedPeriode1.setUuid(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        expectedPeriode1.setStartDato(ZonedDateTime.of(2021, 5, 4, 9, 48, 58, 0, ZoneId.of("+2")).plus(762, MILLIS));
        expectedPeriode1.setSluttDato(ZonedDateTime.of(2021, 6, 4, 9, 48, 58, 0, ZoneId.of("+2")).plus(762, MILLIS));

        OppfolgingPeriodeDTO expectedPeriode2 = new OppfolgingPeriodeDTO();
        expectedPeriode2.setUuid(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
        expectedPeriode2.setStartDato(ZonedDateTime.of(2020, 5, 4, 9, 48, 58, 0, ZoneId.of("+2")).plus(762, MILLIS));
        expectedPeriode2.setSluttDato(ZonedDateTime.of(2020, 6, 4, 9, 48, 58, 0, ZoneId.of("+2")).plus(762, MILLIS));

        List<OppfolgingPeriodeDTO> oppfolgingsperioder = veilarboppfolgingClient.hentOppfolgingsperioder(TEST_FNR);

        assertEquals(2, oppfolgingsperioder.size());
        assertEquals(expectedPeriode1, oppfolgingsperioder.get(0));
        assertEquals(expectedPeriode2, oppfolgingsperioder.get(1));
    }

    @Test
    public void hentGjeldendeOppfolgingsperiode__skal_lage_riktig_request_og_parse_response() {
        String response = TestUtils.readTestResourceFile("testdata/veilarboppfolging_hentGjeldendeOppfolgingsperiode.json");

        givenThat(post(urlEqualTo("/api/v3/oppfolging/hent-gjeldende-periode"))
                .withRequestBody(WireMock.equalToJson("{\"fnr\":\"" + TEST_FNR.get() + "\"}"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer SYSTEM_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(response))
        );

        OppfolgingPeriodeDTO expectedPeriode = new OppfolgingPeriodeDTO();
        expectedPeriode.setUuid(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        expectedPeriode.setStartDato(ZonedDateTime.of(2021, 5, 4, 9, 48, 58, 0, ZoneId.of("+2")).plus(762, MILLIS));
        expectedPeriode.setSluttDato(ZonedDateTime.of(2021, 6, 4, 9, 48, 58, 0, ZoneId.of("+2")).plus(762, MILLIS));

        Optional<OppfolgingPeriodeDTO> gjeldendeOppfolgingsperiode = veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(TEST_FNR);

        assertEquals(Optional.of(expectedPeriode), gjeldendeOppfolgingsperiode);
    }

    @Test
    public void hentGjeldendeOppfolgingsperiode__skal_håndtere_manglende_respons() {

        givenThat(post(urlEqualTo("/api/v3/oppfolging/hent-gjeldende-periode"))
                .withRequestBody(WireMock.equalToJson("{\"fnr\":\"" + TEST_FNR.get() + "\"}"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer SYSTEM_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(204))
        );

        Optional<OppfolgingPeriodeDTO> gjeldendeOppfolgingsperiode =
                veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(TEST_FNR);

        assertEquals(Optional.empty(), gjeldendeOppfolgingsperiode);
    }

    @Test
    public void hentOppfolgingsSak_skal_parse_response() {
        UUID oppfolgingsperiodeId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String response = TestUtils.readTestResourceFile("testdata/veilarboppfolging_hentOppfolgingsperiodeSak.json");

        givenThat(post(urlEqualTo("/api/v3/sak/"+ oppfolgingsperiodeId))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer SYSTEM_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(response))
        );

        SakDTO expectedSakDTO = new SakDTO(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), 123456789012L, "ARBEIDSOPPFOLGING", "OPP");

        SakDTO oppfolgingSak = veilarboppfolgingClient.hentOppfolgingsperiodeSak(oppfolgingsperiodeId);

        assertNotNull(oppfolgingSak);
        assertEquals(expectedSakDTO, oppfolgingSak);
    }
}

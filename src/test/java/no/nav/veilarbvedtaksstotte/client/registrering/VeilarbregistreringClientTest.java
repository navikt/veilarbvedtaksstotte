package no.nav.veilarbvedtaksstotte.client.registrering;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static org.junit.Assert.assertEquals;

public class VeilarbregistreringClientTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    VeilarbregistreringClient veilarbregistreringClient;

    @Before
    public void setup() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        veilarbregistreringClient = new VeilarbregistreringClientImpl(apiUrl, AuthContextHolderThreadLocal.instance());
    }

    @Test
    public void test() {

        String response = TestUtils.readTestResourceFile("registrering.json");

        WireMock.givenThat(
                WireMock.get(WireMock.urlEqualTo("/api/registrering?fnr=" + TEST_FNR))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withBody(response)
                        )
        );
        RegistreringData registreringData = AuthContextHolderThreadLocal
                .instance()
                .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "SUBJECT"), () ->
                        veilarbregistreringClient.hentRegistreringData(TEST_FNR));

        assertEquals(registreringData, new RegistreringData(new RegistreringData.BrukerRegistrering(
                LocalDateTime.of(2021, 1, 18, 9, 48, 58).plus(762, MILLIS),
                new RegistreringData.Profilering(RegistreringData.ProfilertInnsatsgruppe.SITUASJONSBESTEMT_INNSATS))));
    }
}

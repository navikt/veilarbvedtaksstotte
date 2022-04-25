package no.nav.veilarbvedtaksstotte.client.dokarkiv;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.veilarbvedtaksstotte.utils.TestData;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertTrue;

public class SafClientImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void hentJournalposter__skalReturnereJournalposter() {
        String journalposterJson = TestUtils.readTestResourceFile("saf-client-journalposter.json");
        String apiUrl = "http://localhost:" + wireMockRule.port();
        SafClient safClient = new SafClientImpl(apiUrl, AuthContextHolderThreadLocal.instance());

        givenThat(post(urlEqualTo("/graphql"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(journalposterJson))
        );


        AuthContextHolderThreadLocal
                .instance()
                .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "test"), () -> {
            List<Journalpost> journalposter = safClient.hentJournalposter(TestData.TEST_FNR);

            assertTrue(journalposter.stream().anyMatch(j ->
                    "212934817".equals(j.journalpostId) && "417324785".equals(j.dokumenter[0].dokumentInfoId)
            ));
            assertTrue(journalposter.stream().anyMatch(j ->
                    "1133493487".equals(j.journalpostId) && "31235785".equals(j.dokumenter[0].dokumentInfoId)
            ));
        });
    }

    @Test(expected = RuntimeException.class)
    public void hentJournalposter__skalKasteExceptionPaErrorStatus() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        SafClient safClient = new SafClientImpl(apiUrl, AuthContextHolderThreadLocal.instance());

        givenThat(post(urlEqualTo("/graphql")).willReturn(aResponse().withStatus(500)));

        safClient.hentJournalposter(TestData.TEST_FNR);
    }

}

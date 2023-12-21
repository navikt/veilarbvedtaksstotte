package no.nav.veilarbvedtaksstotte.client.dokarkiv;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.Journalpost;
import no.nav.veilarbvedtaksstotte.utils.TestData;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
public class SafClientImplTest {

    @Test
    public void hentJournalposter__skalReturnereJournalposter(WireMockRuntimeInfo wireMockRuntimeInfo) {
        String journalposterJson = TestUtils.readTestResourceFile("testdata/saf-client-journalposter.json");

        String apiUrl = "http://localhost:" + wireMockRuntimeInfo.getHttpPort();
        SafClient safClient = new SafClientImpl(apiUrl, () -> "");

        givenThat(post(urlEqualTo("/graphql"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(journalposterJson))
        );

        List<Journalpost> journalposter = safClient.hentJournalposter(TestData.TEST_FNR);

        assertTrue(journalposter.stream().anyMatch(j ->
                "212934817".equals(j.journalpostId) && "417324785".equals(j.dokumenter[0].dokumentInfoId)
        ));
        assertTrue(journalposter.stream().anyMatch(j ->
                "1133493487".equals(j.journalpostId) && "31235785".equals(j.dokumenter[0].dokumentInfoId)
        ));
    }

    @Test
    public void hentJournalposter__skalKasteExceptionPaErrorStatus(WireMockRuntimeInfo wireMockRuntimeInfo) {
        String apiUrl = "http://localhost:" + wireMockRuntimeInfo.getHttpPort();
        SafClient safClient = new SafClientImpl(apiUrl, () -> "");

        givenThat(post(urlEqualTo("/graphql")).willReturn(aResponse().withStatus(500)));

        assertThrows(RuntimeException.class, () ->
                safClient.hentJournalposter(TestData.TEST_FNR)
        );
    }

    @Test
    public void hentJournalpostById(WireMockRuntimeInfo wireMockRuntimeInfo) {
        String journalpostJson = TestUtils.readTestResourceFile("testdata/saf-client-journalpost.json");

        String apiUrl = "http://localhost:" + wireMockRuntimeInfo.getHttpPort();
        SafClient safClient = new SafClientImpl(apiUrl, () -> "");

        givenThat(post(urlEqualTo("/graphql"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(journalpostJson))
        );

        JournalpostGraphqlResponse journalpost = safClient.hentJournalpost(TestData.TEST_JOURNALPOST_ID);
        assertNotNull(journalpost);
        assertTrue(journalpost.getData().getJournalpost().dokumenter.length > 0);
        assertNotNull(journalpost.getData().getJournalpost().dokumenter[0].dokumentInfoId);
        assertNotNull(journalpost.getData().getJournalpost().dokumenter[0].brevkode);
    }

}

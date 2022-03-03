package no.nav.veilarbvedtaksstotte.client.dokdistfordeling

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.sts.SystemUserTokenProvider
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.utils.fn.UnsafeSupplier
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class DokdistribusjonClientTest {

    lateinit var dokdistribusjonClient: DokdistribusjonClient

    private val wireMockRule = WireMockRule()

    val systemUserTokenProvider: SystemUserTokenProvider = Mockito.mock(SystemUserTokenProvider::class.java)
    val serviceTokenSupplier: () -> String = { "" }

    @Rule
    fun getWireMockRule() = wireMockRule

    @Before
    fun setup() {
        val wiremockUrl = "http://localhost:" + getWireMockRule().port()
        dokdistribusjonClient = DokdistribusjonClientImpl(wiremockUrl, serviceTokenSupplier)
    }

    @Test
    fun `distribuering av journalpost gir forventet innhold i request og response`() {
        val forventetRequest =
            """
                {
                    "bestillendeFagsystem": "BD11",
                    "dokumentProdApp": "VEILARB_VEDTAK14A",
                    "journalpostId": "123"
                }
                """

        val responsJson =
            """
                {
                   "bestillingsId": "BESTILLINGS_ID"
                } 
                """

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/rest/v1/distribuerjournalpost"))
                .withRequestBody(WireMock.equalToJson(forventetRequest))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(201)
                        .withBody(responsJson)
                )
        )

        val respons =
            AuthContextHolderThreadLocal
                .instance()
                .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "SUBJECT"), UnsafeSupplier {
                    dokdistribusjonClient.distribuerJournalpost(
                        DistribuerJournalpostDTO(
                            journalpostId = "123",
                            bestillendeFagsystem = "BD11",
                            dokumentProdApp = "VEILARB_VEDTAK14A"
                        )
                    )
                })

        Assert.assertEquals("BESTILLINGS_ID", respons?.bestillingsId)
    }
}

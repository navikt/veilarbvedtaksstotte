package no.nav.veilarbvedtaksstotte.client.dokdistfordeling

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.dto.DistribuerJournalpostDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@WireMockTest
class DokdistribusjonClientTest {

    companion object {
        lateinit var dokdistribusjonClient: DokdistribusjonClient

        @BeforeAll
        @JvmStatic
        fun setup(wireMockRuntimeInfo: WireMockRuntimeInfo) {
            dokdistribusjonClient = DokdistribusjonClientImpl("http://localhost:" + wireMockRuntimeInfo.httpPort) { "" }
        }
    }

    @Test
    fun `distribuering av journalpost gir forventet innhold i request og response`() {
        val forventetRequest =
            """
                {
                    "bestillendeFagsystem": "BD11",
                    "dokumentProdApp": "VEILARB_VEDTAK14A",
                    "journalpostId": "123",
                    "distribusjonstype": "VEDTAK",
                    "distribusjonstidspunkt": "KJERNETID"
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
                            dokumentProdApp = "VEILARB_VEDTAK14A",
                            distribusjonstype = "VEDTAK",
                            distribusjonstidspunkt = "KJERNETID"
                        )
                    )
                })

        assertEquals("BESTILLINGS_ID", respons?.bestillingsId)
    }
}

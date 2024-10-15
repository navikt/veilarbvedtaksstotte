package no.nav.veilarbvedtaksstotte.client.arena

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSSAK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@WireMockTest
class VeilarbarenaClientImplTest {


    companion object {
        lateinit var veilarbarenaClient: VeilarbarenaClient

        @BeforeAll
        @JvmStatic
        fun setup(wireMockRuntimeInfo: WireMockRuntimeInfo) {
            veilarbarenaClient = VeilarbarenaClientImpl("http://localhost:" + wireMockRuntimeInfo.httpPort) { "" }
        }
    }

    @Test
    fun `hent oppfolgingssak gir forventet respons`() {

        val response =
            """
                {
                    "oppfolgingssakId": "$TEST_OPPFOLGINGSSAK"
                }
            """

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v2/hent-oppfolgingssak"))
                .withRequestBody(WireMock.equalToJson("{\"fnr\":\"$TEST_FNR\"}"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(response)
                )
        )

        val oppfolgingssak = veilarbarenaClient.oppfolgingssak(TEST_FNR).get()

        assertEquals(oppfolgingssak, TEST_OPPFOLGINGSSAK)
    }

    @Test
    fun `hent oppfoglingssak feiler dersom respons er 204`() {

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v2/hent-oppfolgingssak"))
                .withRequestBody(WireMock.equalToJson("{\"fnr\":\"$TEST_FNR\"}"))
                .willReturn(
                    WireMock.noContent()
                )
        )

        assertThrows(IllegalStateException::class.java) {
            veilarbarenaClient.oppfolgingssak(TEST_FNR)
        }
    }

    @Test
    fun `hent oppfoglingssak er tom dersom respons er 404`() {

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v2/hent-oppfolgingssak"))
                .withRequestBody(WireMock.equalToJson("{\"fnr\":\"$TEST_FNR\"}"))
                .willReturn(
                    WireMock.notFound()
                )
        )

        val oppfolgingssak = veilarbarenaClient.oppfolgingssak(TEST_FNR)

        assertTrue(oppfolgingssak.isEmpty)
    }
}

package no.nav.veilarbvedtaksstotte.client.arena

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.veilarbvedtaksstotte.client.arena.dto.VeilarbArenaOppfolging
import no.nav.veilarbvedtaksstotte.utils.TestData.*
import no.nav.veilarbvedtaksstotte.utils.toJson
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
    fun `hent oppfolgingsbruker gir forventet respons`() {
        val response =
            """
                {
                    "navKontor": "$TEST_NAVKONTOR"
                }
            """

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v3/hent-oppfolgingsbruker"))
                .withRequestBody(WireMock.equalToJson("{\"fnr\":\"$TEST_FNR\"}"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(response)
                )
        )

        val oppfolgingsbruker = veilarbarenaClient.hentOppfolgingsbruker(TEST_FNR).get()
        assertEquals(oppfolgingsbruker.navKontor, TEST_NAVKONTOR)
    }

    @Test
    fun `hent oppdatert oppfolgingsbruker gir forventet respons`() {
        val response =
            """
                {
                    "navKontor": "$TEST_NAVKONTOR"
                }
            """
        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v3/hent-oppfolgingsbruker"))
                .withRequestBody(WireMock.equalToJson("{\"fnr\":\"$TEST_FNR\"}"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(response)
                )
        )

        veilarbarenaClient.hentOppfolgingsbruker(TEST_FNR)
        val oppdatertOppfolgingsbruker = veilarbarenaClient.oppdaterOppfolgingsbruker(TEST_FNR, TEST_OPPDATERT_NAVKONTOR)

        assertEquals(TEST_OPPDATERT_NAVKONTOR, oppdatertOppfolgingsbruker.get().navKontor)

    }
}

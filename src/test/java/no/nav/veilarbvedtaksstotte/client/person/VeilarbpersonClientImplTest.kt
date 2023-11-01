package no.nav.veilarbvedtaksstotte.client.person

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.Målform
import no.nav.veilarbvedtaksstotte.utils.JsonUtils.createNoDataStr
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR
import no.nav.veilarbvedtaksstotte.utils.TestUtils
import no.nav.veilarbvedtaksstotte.utils.TestUtils.givenWiremockOkJsonResponse
import no.nav.veilarbvedtaksstotte.utils.TestUtils.givenWiremockOkJsonResponseForPost
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@WireMockTest
class VeilarbpersonClientImplTest  {

    companion object {
        lateinit var veilarbpersonClient: VeilarbpersonClient

        @BeforeAll
        @JvmStatic
        fun setup(wireMockRuntimeInfo: WireMockRuntimeInfo) {
            veilarbpersonClient = VeilarbpersonClientImpl("http://localhost:" + wireMockRuntimeInfo.httpPort) { "" }
        }
    }
    @Test
    fun skal_hente_person() {
        WireMock.givenThat(
            WireMock.post("/api/v3/person/hent-navn").withRequestBody(WireMock.equalToJson("{\"fnr\":\"$TEST_FNR\"}")).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("Authorization", "Bearer TOKEN").withBody(
                    """
                               {
                                "fornavn": "Fornavn",
                                "mellomnavn": "Mellomnavn",
                                "etternavn": "Etternavn",
                                "forkortetNavn": "Sammensatt Navn"
                               }
                            """
                )
            )
        )
        val hentPersonNavn = veilarbpersonClient.hentPersonNavn(TEST_FNR.get())
        assertEquals(
            PersonNavn(
                fornavn = "Fornavn",
                mellomnavn = "Mellomnavn",
                etternavn = "Etternavn",
                forkortetNavn = "Sammensatt Navn"
            ), hentPersonNavn
        )
    }

    @Test
    fun skal_hente_cv_jobbprofil_json() {
        val cvJobbprofilJson = TestUtils.readTestResourceFile("cv-jobbprofil.json")
        WireMock.givenThat(
            WireMock.post("/api/v3/person/hent-cv_jobbprofil").withRequestBody(WireMock.equalToJson("{\"fnr\":\"1234\"}")).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("Authorization", "Bearer TOKEN")
                    .withBody(cvJobbprofilJson)
            )
        )
        val jsonResponse = veilarbpersonClient.hentCVOgJobbprofil("1234")
        assertEquals(cvJobbprofilJson, jsonResponse)
    }

    @Test
    fun skal_returnere_no_data_json_for_403_og_401() {
        val expectedJsonResponse = createNoDataStr("Bruker har ikke delt CV/jobbprofil med NAV")
        WireMock.givenThat(
            WireMock.post("/api/v3/person/hent-cv_jobbprofil").withRequestBody(WireMock.equalToJson("{\"fnr\":\"1234\"}")).willReturn(WireMock.aResponse().withStatus(401))
        )
        assertEquals(expectedJsonResponse, veilarbpersonClient.hentCVOgJobbprofil("1234"))
        WireMock.givenThat(
            WireMock.post("/api/v3/person/hent-cv_jobbprofil").withRequestBody(WireMock.equalToJson("{\"fnr\":\"1234\"}")).willReturn(WireMock.aResponse().withStatus(403))
        )
        assertEquals(expectedJsonResponse, veilarbpersonClient.hentCVOgJobbprofil("1234"))
    }

    @Test
    fun skal_returnere_no_data_json_for_204_og_404() {
        val expectedJsonResponse = createNoDataStr("Bruker har ikke fylt ut CV/jobbprofil")
        WireMock.givenThat(
            WireMock.post("/api/v3/person/hent-cv_jobbprofil").withRequestBody(WireMock.equalToJson("{\"fnr\":\"1234\"}")).willReturn(WireMock.aResponse().withStatus(204))
        )
        assertEquals(expectedJsonResponse, veilarbpersonClient.hentCVOgJobbprofil("1234"))
        WireMock.givenThat(
            WireMock.post("/api/v3/person/hent-cv_jobbprofil").withRequestBody(WireMock.equalToJson("{\"fnr\":\"1234\"}")).willReturn(WireMock.aResponse().withStatus(404))
        )
        assertEquals(expectedJsonResponse, veilarbpersonClient.hentCVOgJobbprofil("1234"))
    }

    @Test
    fun skal_sjekke_helse() {
        WireMock.givenThat(WireMock.get("/internal/isAlive").willReturn(WireMock.aResponse().withStatus(200)))
        veilarbpersonClient.checkHealth()
    }

    @Test
    fun mapper_respons_riktig() {
        val fnr = Fnr("123")
        val jsonResponse = """{"malform": "NN"}"""

        givenWiremockOkJsonResponseForPost("/api/v3/person/hent-malform", WireMock.equalToJson("{\"fnr\":\"123\"}"), jsonResponse)

        val respons = veilarbpersonClient.hentMålform(fnr)

        assertEquals(Målform.NN, respons)
    }
}

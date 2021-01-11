package no.nav.veilarbvedtaksstotte.client.person

import no.nav.veilarbvedtaksstotte.utils.JsonUtils.createNoDataStr
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR
import no.nav.veilarbvedtaksstotte.utils.TestUtils
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class VeilarbpersonClientImplTest {

    lateinit var veilarbpersonClient: VeilarbpersonClient

    private val wireMockRule = WireMockRule()

    @Rule
    fun getWireMockRule() = wireMockRule

    @Before
    fun setup() {
        val wiremockUrl = "http://localhost:" + getWireMockRule().port()
        veilarbpersonClient = VeilarbpersonClientImpl(wiremockUrl) { "TOKEN" }
    }

    @Test
    fun skal_hente_person() {
        WireMock.givenThat(
            WireMock.get("/api/person/navn?fnr=$TEST_FNR")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Authorization", "Bearer TOKEN")
                        .withBody(
                            """
                               {
                                "fornavn": "Fornavn",
                                "mellomnavn": "Mellomnavn",
                                "etternavn": "Etternavn",
                                "sammensattNavn": "Sammensatt Navn"
                               } 
                            """
                        )
                )
        )
        val hentPersonNavn = veilarbpersonClient.hentPersonNavn(TEST_FNR)
        assertEquals(
            PersonNavn(
                fornavn = "Fornavn",
                mellomnavn = "Mellomnavn",
                etternavn = "Etternavn",
                sammensattNavn = "Sammensatt Navn"
            ), hentPersonNavn
        )
    }

    @Test
    fun skal_hente_cv_jobbprofil_json() {
        val cvJobbprofilJson = TestUtils.readTestResourceFile("cv-jobbprofil.json")
        WireMock.givenThat(
            WireMock.get("/api/person/cv_jobbprofil?fnr=1234")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Authorization", "Bearer TOKEN")
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
            WireMock.get("/api/person/cv_jobbprofil?fnr=1234").willReturn(WireMock.aResponse().withStatus(401))
        )
        assertEquals(expectedJsonResponse, veilarbpersonClient.hentCVOgJobbprofil("1234"))
        WireMock.givenThat(
            WireMock.get("/api/person/cv_jobbprofil?fnr=1234").willReturn(WireMock.aResponse().withStatus(403))
        )
        assertEquals(expectedJsonResponse, veilarbpersonClient.hentCVOgJobbprofil("1234"))
    }

    @Test
    fun skal_returnere_no_data_json_for_204_og_404() {
        val expectedJsonResponse = createNoDataStr("Bruker har ikke fylt ut CV/jobbprofil")
        WireMock.givenThat(
            WireMock.get("/api/person/cv_jobbprofil?fnr=1234").willReturn(WireMock.aResponse().withStatus(204))
        )
        assertEquals(expectedJsonResponse, veilarbpersonClient.hentCVOgJobbprofil("1234"))
        WireMock.givenThat(
            WireMock.get("/api/person/cv_jobbprofil?fnr=1234").willReturn(WireMock.aResponse().withStatus(404))
        )
        assertEquals(expectedJsonResponse, veilarbpersonClient.hentCVOgJobbprofil("1234"))
    }

    @Test
    fun skal_sjekke_helse() {
        WireMock.givenThat(WireMock.get("/internal/isAlive").willReturn(WireMock.aResponse().withStatus(200)))
        veilarbpersonClient.checkHealth()
    }
}

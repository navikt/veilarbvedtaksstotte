package no.nav.veilarbvedtaksstotte.client.person

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.person.dto.CvDto
import no.nav.veilarbvedtaksstotte.client.person.dto.CvErrorStatus
import no.nav.veilarbvedtaksstotte.client.person.dto.CvInnhold
import no.nav.veilarbvedtaksstotte.client.person.dto.PersonNavn
import no.nav.veilarbvedtaksstotte.domain.Malform
import no.nav.veilarbvedtaksstotte.utils.JsonUtils
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR
import no.nav.veilarbvedtaksstotte.utils.TestUtils
import no.nav.veilarbvedtaksstotte.utils.TestUtils.givenWiremockOkJsonResponseForPost
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

@WireMockTest
class VeilarbpersonClientImplTest {

    companion object {
        lateinit var veilarbpersonClient: VeilarbpersonClient

        @BeforeAll
        @JvmStatic
        fun setup(wireMockRuntimeInfo: WireMockRuntimeInfo) {
            veilarbpersonClient = VeilarbpersonClientImpl("http://localhost:" + wireMockRuntimeInfo.httpPort, {""},{""})
        }
    }

    @Test
    fun skal_hente_person() {
        WireMock.givenThat(
            WireMock.post("/api/v3/person/hent-navn")
                .withRequestBody(WireMock.equalToJson("{\"fnr\":\"$TEST_FNR\", \"behandlingsnummer\": \"" + BehandlingsNummer.VEDTAKSTOTTE.value + "\"}"))
                .willReturn(
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
        val cvJobbprofilJson = TestUtils.readTestResourceFile("testdata/cv-jobbprofil.json")
        WireMock.givenThat(
            WireMock.post("/api/v3/person/hent-cv_jobbprofil")
                .withRequestBody(WireMock.equalToJson("{\"fnr\":\"1234\", \"behandlingsnummer\": \"" + BehandlingsNummer.VEDTAKSTOTTE.value + "\"}"))
                .willReturn(
                    WireMock.aResponse().withStatus(200).withHeader("Authorization", "Bearer TOKEN")
                        .withBody(cvJobbprofilJson)
                )
        )
        val cvDto = veilarbpersonClient.hentCVOgJobbprofil("1234")
        when (cvDto) {
            is CvDto.CVMedInnhold -> {
                Assertions.assertEquals(
                    JsonUtils.fromJson(cvJobbprofilJson, CvInnhold::class.java),
                    cvDto.cvInnhold
                )
            }

            is CvDto.CvMedError -> {
                fail("Unexpected error")
            }

            else -> fail("Unexpected error")
        }

    }

    @Test
    fun skal_returnere_no_data_json_for_403_og_401() {
        WireMock.givenThat(
            WireMock.post("/api/v3/person/hent-cv_jobbprofil")
                .withRequestBody(WireMock.equalToJson("{\"fnr\":\"1234\", \"behandlingsnummer\": \"" + BehandlingsNummer.VEDTAKSTOTTE.value + "\"}"))
                .willReturn(WireMock.aResponse().withStatus(401))
        )

        var cvDto = veilarbpersonClient.hentCVOgJobbprofil("1234")
        when (cvDto) {
            is CvDto.CVMedInnhold -> {
                fail("Unexpected error")
            }

            is CvDto.CvMedError -> {
                Assertions.assertEquals(cvDto.cvErrorStatus, CvErrorStatus.IKKE_DELT)
            }

            else -> fail("Unexpected error")
        }


        WireMock.givenThat(
            WireMock.post("/api/v3/person/hent-cv_jobbprofil")
                .withRequestBody(WireMock.equalToJson("{\"fnr\":\"1234\", \"behandlingsnummer\": \"" + BehandlingsNummer.VEDTAKSTOTTE.value + "\"}"))
                .willReturn(WireMock.aResponse().withStatus(403))
        )
        cvDto = veilarbpersonClient.hentCVOgJobbprofil("1234")
        when (cvDto) {
            is CvDto.CVMedInnhold -> {
                fail("Unexpected error")
            }

            is CvDto.CvMedError -> {
                Assertions.assertEquals(cvDto.cvErrorStatus, CvErrorStatus.IKKE_DELT)
            }

            else -> fail("Unexpected error")
        }
    }

    @Test
    fun skal_returnere_no_data_json_for_204_og_404() {
        WireMock.givenThat(
            WireMock.post("/api/v3/person/hent-cv_jobbprofil")
                .withRequestBody(WireMock.equalToJson("{\"fnr\":\"1234\", \"behandlingsnummer\": \"" + BehandlingsNummer.VEDTAKSTOTTE.value + "\"}"))
                .willReturn(WireMock.aResponse().withStatus(204))
        )
        var cvDto = veilarbpersonClient.hentCVOgJobbprofil("1234")
        when (cvDto) {
            is CvDto.CVMedInnhold -> {
                fail("Unexpected error")
            }

            is CvDto.CvMedError -> {
                Assertions.assertEquals(cvDto.cvErrorStatus, CvErrorStatus.IKKE_FYLT_UT)
            }

            else -> fail("Unexpected error")
        }

        WireMock.givenThat(
            WireMock.post("/api/v3/person/hent-cv_jobbprofil")
                .withRequestBody(WireMock.equalToJson("{\"fnr\":\"1234\", \"behandlingsnummer\": \"" + BehandlingsNummer.VEDTAKSTOTTE.value + "\"}"))
                .willReturn(WireMock.aResponse().withStatus(404))
        )
        cvDto = veilarbpersonClient.hentCVOgJobbprofil("1234")
        when (cvDto) {
            is CvDto.CVMedInnhold -> {
                fail("Unexpected error")
            }

            is CvDto.CvMedError -> {
                Assertions.assertEquals(cvDto.cvErrorStatus, CvErrorStatus.IKKE_FYLT_UT)
            }

            else -> fail("Unexpected error")
        }
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

        givenWiremockOkJsonResponseForPost(
            "/api/v3/person/hent-malform",
            WireMock.equalToJson("{\"fnr\":\"123\", \"behandlingsnummer\": \"" + BehandlingsNummer.VEDTAKSTOTTE.value + "\"}"),
            jsonResponse
        )

        val respons = veilarbpersonClient.hentMalform(fnr)

        assertEquals(Malform.NN, respons)
    }
}

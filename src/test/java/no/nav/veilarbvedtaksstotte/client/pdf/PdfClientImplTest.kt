package no.nav.veilarbvedtaksstotte.client.pdf

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.dokument.MalType
import no.nav.veilarbvedtaksstotte.domain.Malform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@WireMockTest
class PdfClientImplTest {

    companion object {
        lateinit var pdfClient: PdfClient

        @BeforeAll
        @JvmStatic
        fun setup(wireMockRuntimeInfo: WireMockRuntimeInfo) {
            pdfClient = PdfClientImpl("http://localhost:" + wireMockRuntimeInfo.httpPort)
        }
    }

    @Test
    fun request_til_brev_client_har_forventet_innhold() {

        val brevdata = BrevdataDto(
            malType = MalType.SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID,
            veilederNavn = "Veileder Navn",
            navKontor = "Nav kontor",
            dato = "20. januar 2020",
            malform = Malform.NB,
            begrunnelse = listOf("Avsnitt 1", "Avsnitt 2"),
            kilder = listOf("Kilde 1", "Kilde 2"),
            mottaker = Mottaker(
                navn = "Mottaker Navn",
                fodselsnummer = Fnr.ofValidFnr("12345678910"),
            ),
            utkast = false,
            ungdomsgaranti = true
        )

        val documentResponse = "document"

        val forventetInnhold =
            """
                    {
                      "malType": "SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID",
                      "veilederNavn": "Veileder Navn",
                      "navKontor": "Nav kontor",
                      "dato": "20. januar 2020",
                      "malform": "NB",
                      "begrunnelse": ["Avsnitt 1", "Avsnitt 2"],
                      "kilder": ["Kilde 1", "Kilde 2"],
                      "mottaker": {
                        "navn": "Mottaker Navn",
                        "fodselsnummer": "12345678910"
                      },
                      "utkast": false,
                      "ungdomsgaranti": true
                    }
                """


        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v1/genpdf/vedtak14a/vedtak14a"))
                .withRequestBody(WireMock.equalToJson(forventetInnhold))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(documentResponse.toByteArray())
                        .withHeader("Content-Type", "application/pdf")
                )
        )


        val response = pdfClient.genererPdf(brevdata)

        assertEquals(documentResponse, response.decodeToString())
    }

    @Test
    fun `ugyldige tegn skal bli fjernet fra tekstinput til pdfgen`() {
        val ugyldigInput0002 = "Hello\u0002World\nLine2\u0000"
        val forventet0002 = "HelloWorld\nLine2"
        val vasket0002 = vaskStringForUgyldigeTegn(ugyldigInput0002)
        assertEquals(forventet0002, vasket0002)

        val ugyldigFEFF = "Hello\uFEFFWorld Line2\uFEFF test\uFEFF"
        val forventetVasketFEFF = "HelloWorld Line2 test"
        val vasketFEFF = vaskStringForUgyldigeTegn(ugyldigFEFF)
        assertEquals(forventetVasketFEFF, vasketFEFF)
    }
}


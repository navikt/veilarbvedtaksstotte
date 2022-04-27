package no.nav.veilarbvedtaksstotte.client.pdf

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.veilarbvedtaksstotte.client.dokument.MalType
import no.nav.veilarbvedtaksstotte.domain.Målform
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PdfClientImplTest {

    lateinit var pdfClient: PdfClient

    private val wireMockRule = WireMockRule()

    @Rule
    fun getWireMockRule() = wireMockRule

    @Before
    fun setup() {
        pdfClient = PdfClientImpl("http://localhost:" + getWireMockRule().port())
    }

    @Test
    fun request_til_brev_client_har_forventet_innhold() {

        val brevdata = PdfClient.Brevdata(
            malType = MalType.SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID,
            veilederNavn = "Veileder Navn",
            navKontor = "Nav kontor",
            kontaktEnhetNavn = "Nav kontor kontakt",
            kontaktTelefonnummer = "00000000",
            dato = "20. januar 2020",
            malform = Målform.NB,
            begrunnelse = listOf("Avsnitt 1", "Avsnitt 2"),
            kilder = listOf("Kilde 1", "Kilde 2"),
            mottaker = PdfClient.Mottaker(
                navn = "Mottaker Navn",
                adresselinje1 = "Adresselinje 1",
                adresselinje2 = "Adresselinje 2",
                adresselinje3 = "Adresselinje 3",
                postnummer = "0000",
                poststed = "Sted",
                land = "Sverige"
            ),
            postadresse = PdfClient.Adresse(
                adresselinje = "Retur adresselinje",
                postnummer = "4321",
                poststed = "Retur poststed"
            ),
            utkast = false
        )

        val documentResponse = "document"

        val forventetInnhold =
            """
                    {
                      "malType": "SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID",
                      "veilederNavn": "Veileder Navn",
                      "navKontor": "Nav kontor",
                      "kontaktTelefonnummer": "00000000",
                      "kontaktEnhetNavn": "Nav kontor kontakt",
                      "dato": "20. januar 2020",
                      "malform": "NB",
                      "begrunnelse": ["Avsnitt 1", "Avsnitt 2"],
                      "kilder": ["Kilde 1", "Kilde 2"],
                      "mottaker": {
                        "navn": "Mottaker Navn",
                        "adresselinje1": "Adresselinje 1",
                        "adresselinje2": "Adresselinje 2",
                        "adresselinje3": "Adresselinje 3",
                        "postnummer": "0000",
                        "poststed": "Sted",
                        "land": "Sverige"
                      },
                      "postadresse": {
                        "adresselinje": "Retur adresselinje",
                        "postnummer": "4321",
                        "poststed": "Retur poststed"
                      },
                      "utkast": false
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
}


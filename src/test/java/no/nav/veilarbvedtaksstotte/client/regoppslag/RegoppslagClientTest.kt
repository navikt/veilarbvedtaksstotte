package no.nav.veilarbvedtaksstotte.client.regoppslag

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.test.auth.AuthTestUtils.createAuthContext
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO.AdresseType.NORSKPOSTADRESSE
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@WireMockTest
class RegoppslagClientTest {

    companion object {
        lateinit var regoppslagClient: RegoppslagClient

        @BeforeAll
        @JvmStatic
        fun setup(wireMockRuntimeInfo: WireMockRuntimeInfo) {
            regoppslagClient =
                RegoppslagClientImpl("http://localhost:" + wireMockRuntimeInfo.httpPort) { "SYSTEM_USER_TOKEN" }
        }
    }

    @Test
    fun `regoppslag gir forventet innhold i request og response`() {
        val forventetRequest =
            """
                {
                  "ident": "$TEST_FNR",
                  "tema": "OPP"
                }
            """.trimIndent()

        val responsJson =
            """
                {
                  "navn": "Navn Navnesen",
                  "adresse": {
                    "type": "NORSKPOSTADRESSE",
                    "adresselinje1": "Adresselinje 1",
                    "adresselinje2": "Adresselinje 2",
                    "adresselinje3": "Adresselinje 3",
                    "postnummer": "0000",
                    "poststed": "Sted",
                    "landkode": "NO",
                    "land": "Norge"
                  }
                }
            """.trimIndent()

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/rest/postadresse"))
                .withRequestBody(WireMock.equalToJson(forventetRequest))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(responsJson)
                )
        )

        val respons =
            AuthContextHolderThreadLocal
                .instance()
                .withContext(createAuthContext(UserRole.INTERN, "SUBJECT"), UnsafeSupplier {
                    regoppslagClient.hentPostadresse(
                        RegoppslagRequestDTO(TEST_FNR.get(), "OPP")
                    )
                })

        val forventetRespons = RegoppslagResponseDTO(
            navn = "Navn Navnesen",
            adresse = RegoppslagResponseDTO.Adresse(
                type = NORSKPOSTADRESSE,
                adresselinje1 = "Adresselinje 1",
                adresselinje2 = "Adresselinje 2",
                adresselinje3 = "Adresselinje 3",
                postnummer = "0000",
                poststed = "Sted",
                landkode = "NO",
                land = "Norge"
            )
        )

        assertEquals(forventetRespons, respons)
    }
}

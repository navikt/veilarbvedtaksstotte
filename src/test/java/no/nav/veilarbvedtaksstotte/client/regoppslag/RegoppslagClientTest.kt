package no.nav.veilarbvedtaksstotte.client.regoppslag

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.sts.SystemUserTokenProvider
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO.AdresseType.NORSKPOSTADRESSE
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class RegoppslagClientTest {

    lateinit var regoppslagClient: RegoppslagClient

    private val wireMockRule = WireMockRule()

    val systemUserTokenProvider: SystemUserTokenProvider = Mockito.mock(SystemUserTokenProvider::class.java)

    @Rule
    fun getWireMockRule() = wireMockRule

    @Before
    fun setup() {
        val wiremockUrl = "http://localhost:" + getWireMockRule().port()
        regoppslagClient = RegoppslagClientImpl(wiremockUrl, systemUserTokenProvider)
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

        Mockito.`when`(systemUserTokenProvider.getSystemUserToken()).thenReturn("SYSTEM_USER_TOKEN")

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
                .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "SUBJECT"), UnsafeSupplier {
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

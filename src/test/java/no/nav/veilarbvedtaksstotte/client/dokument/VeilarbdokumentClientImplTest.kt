package no.nav.veilarbvedtaksstotte.client.dokument

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.types.identer.EnhetId
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.veilarbvedtaksstotte.client.dokument.MalType.SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID
import no.nav.veilarbvedtaksstotte.client.dokument.ProduserDokumentDTO.AdresseDTO
import no.nav.veilarbvedtaksstotte.utils.TestData.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class VeilarbdokumentClientImplTest {

    lateinit var veilarbdokumentClient: VeilarbdokumentClient

    private val wireMockRule = WireMockRule()

    @Rule
    fun getWireMockRule() = wireMockRule

    @Before
    fun setup() {
        val wiremockUrl = "http://localhost:" + getWireMockRule().port()
        veilarbdokumentClient = VeilarbdokumentClientImpl(wiremockUrl, AuthContextHolderThreadLocal.instance())
    }

    @Test
    fun `produser dokument gir forventet respons`() {

        val produserDokumentDTO =
            ProduserDokumentDTO(
                brukerFnr = TEST_FNR,
                navn = "Navn Navnesen",
                malType = SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID,
                enhetId = EnhetId(TEST_OPPFOLGINGSENHET_ID),
                begrunnelse = TEST_BEGRUNNELSE,
                opplysninger = TEST_KILDER,
                utkast = true,
                adresse = AdresseDTO(
                            adresselinje1 = "Adresselinje 1",
                            adresselinje2 = "Adresselinje 2",
                            adresselinje3 = "Adresselinje 3",
                            postnummer = "0000",
                            poststed = "Sted",
                            land = "Sverige"
                )
            )

        val forventetProduserDokumentJson =
            """
                {
                    "brukerFnr": "$TEST_FNR",
                    "navn": "Navn Navnesen",
                    "malType": "SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID",
                    "enhetId": "$TEST_OPPFOLGINGSENHET_ID",
                    "begrunnelse": "$TEST_BEGRUNNELSE",
                    "opplysninger": ${TEST_KILDER.map { """"$it"""" }},
                    "utkast": true,
                    "adresse": {
                        "adresselinje1": "Adresselinje 1",
                        "adresselinje2": "Adresselinje 2",
                        "adresselinje3": "Adresselinje 3",
                        "postnummer": "0000",
                        "poststed": "Sted",
                        "land": "Sverige"
                    }
                }
            """

        val dokumentRespons = "Dokumentrespons"

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v2/produserdokument"))
                .withRequestBody(WireMock.equalToJson(forventetProduserDokumentJson))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(dokumentRespons)
                )
        )

        val respons = AuthContextHolderThreadLocal
            .instance()
            .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "SUBJECT"), UnsafeSupplier {
                veilarbdokumentClient.produserDokument(produserDokumentDTO)
            })

        assertEquals(dokumentRespons, respons.decodeToString())
    }
}

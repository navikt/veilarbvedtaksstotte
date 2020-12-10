package no.nav.veilarbvedtaksstotte.client.dokument

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.UserRole
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.veilarbvedtaksstotte.client.dokument.MalType.SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID
import no.nav.veilarbvedtaksstotte.utils.TestData.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class VeilarbdokumentClientImplTest {

    lateinit var veilarbdokumentClient: VeilarbdokumentClient

    private val wireMockRule = WireMockRule()

    val sendDokumentDTO: SendDokumentDTO = SendDokumentDTO()
        .setBrukerFnr(TEST_FNR)
        .setMalType(SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID)
        .setEnhetId(TEST_OPPFOLGINGSENHET_ID)
        .setBegrunnelse(TEST_BEGRUNNELSE)
        .setOpplysninger(TEST_KILDER)

    val forventetSendDokumentJson =
        """
            {
                "brukerFnr": "$TEST_FNR",
                "malType": "$SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID",
                "enhetId": "$TEST_OPPFOLGINGSENHET_ID",
                "begrunnelse": "$TEST_BEGRUNNELSE",
                "opplysninger": ${TEST_KILDER.map { """"$it"""" }}
            }
        """

    @Rule
    fun getWireMockRule() = wireMockRule

    @Before
    fun setup() {
        val wiremockUrl = "http://localhost:" + getWireMockRule().port()
        veilarbdokumentClient = VeilarbdokumentClientImpl(wiremockUrl)
    }

    @Test
    fun `produser dokumentutkast gir forventet respons`() {

        val utkastRespons = "Dokumentutkast-respons"

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/dokumentutkast"))
                .withRequestBody(WireMock.equalToJson(forventetSendDokumentJson))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(utkastRespons.toByteArray())
                )
        )

        val respons = AuthContextHolder
            .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "SUBJECT"), UnsafeSupplier {
                veilarbdokumentClient.produserDokumentUtkast(sendDokumentDTO)
            })

        assertEquals(utkastRespons, respons.decodeToString())
    }

    @Test
    fun `send dokument gir forventet respons`() {

        val dokumentSendtRespons =
            """
                 {
                    "journalpostId": "$TEST_JOURNALPOST_ID",
                    "dokumentId": "$TEST_DOKUMENT_ID"
                 }
            """


        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/bestilldokument"))
                .withRequestBody(WireMock.equalToJson(forventetSendDokumentJson))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(dokumentSendtRespons)
                )
        )

        val respons = AuthContextHolder
            .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "SUBJECT"), UnsafeSupplier {
                veilarbdokumentClient.sendDokument(sendDokumentDTO)
            })

        assertEquals(DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID), respons)
    }

    @Test
    fun `produser dokument gir forventet respons`() {

        val produserDokumentV2DTO =
            ProduserDokumentV2DTO()
                .setBrukerFnr(TEST_FNR)
                .setMalType(SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID)
                .setEnhetId(TEST_OPPFOLGINGSENHET_ID)
                .setBegrunnelse(TEST_BEGRUNNELSE)
                .setOpplysninger(TEST_KILDER)
                .setUtkast(true)

        val forventetProduserDokumentV2Json =
            """
                {
                    "brukerFnr": "$TEST_FNR",
                    "malType": "$SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID",
                    "enhetId": "$TEST_OPPFOLGINGSENHET_ID",
                    "begrunnelse": "$TEST_BEGRUNNELSE",
                    "opplysninger": ${TEST_KILDER.map { """"$it"""" }},
                    "utkast": true
                }
            """

        val dokumentRespons = "Dokumentrespons"

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v2/produserdokument"))
                .withRequestBody(WireMock.equalToJson(forventetProduserDokumentV2Json))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(dokumentRespons)
                )
        )

        val respons = AuthContextHolder
            .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "SUBJECT"), UnsafeSupplier {
                veilarbdokumentClient.produserDokumentV2(produserDokumentV2DTO)
            })

        assertEquals(dokumentRespons, respons.decodeToString())
    }
}

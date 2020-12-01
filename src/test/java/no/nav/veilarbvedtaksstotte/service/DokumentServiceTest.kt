package no.nav.veilarbvedtaksstotte.service

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.nimbusds.jose.util.Base64
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.UserRole
import no.nav.common.sts.SystemUserTokenProvider
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClientImpl
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettetJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DistribuerJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DistribuerJournalpostResponsDTO
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient
import no.nav.veilarbvedtaksstotte.client.dokument.VeilarbdokumentClient
import no.nav.veilarbvedtaksstotte.client.dokument.VeilarbdokumentClientImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class DokumentServiceTest {

    lateinit var veilarbdokumentClient: VeilarbdokumentClient
    lateinit var dokdistribusjonClient: DokdistribusjonClient
    lateinit var dokarkivClient: DokarkivClient
    lateinit var dokumentService: DokumentService

    private val wireMockRule = WireMockRule()

    val systemUserTokenProvider: SystemUserTokenProvider = mock(SystemUserTokenProvider::class.java)

    @Rule
    fun getWireMockRule() = wireMockRule

    @Before
    fun setup() {
        val wiremockUrl = "http://localhost:" + getWireMockRule().port()
        dokarkivClient = DokarkivClientImpl(wiremockUrl, systemUserTokenProvider)
        veilarbdokumentClient = VeilarbdokumentClientImpl(wiremockUrl)
        dokdistribusjonClient = object : DokdistribusjonClient {
            override fun distribuerJournalpost(request: DistribuerJournalpostDTO): DistribuerJournalpostResponsDTO {
                TODO("Not yet implemented")
            }
        }
        dokumentService = DokumentService(veilarbdokumentClient, dokarkivClient, dokdistribusjonClient)
    }


    @Test
    fun `journalforing av dokument gir forventet innhold i request og response`() {
        val forventetDokument = "dokument".toByteArray()
        val forventetRequest =
                """
                {
                  "tittel": "Tittel",
                  "journalpostType": "UTGAAENDE",
                  "tema": "OPP",
                  "journalfoerendeEnhet": "ENHET_ID",
                  "avsenderMottaker": {
                    "id": "fnr",
                    "idType": "FNR"
                  },
                  "bruker": {
                    "id": "fnr",
                    "idType": "FNR"
                  },
                  "sak": {
                    "fagsakId": "OPPF_SAK",
                    "fagsaksystem": "AO01",
                    "sakstype": "FAGSAK"
                  },
                  "dokumenter": [
                    {
                      "tittel": "Tittel",
                      "brevkode": "BREV_KODE",
                      "dokumentvarianter": [
                        {
                          "filtype": "PDFA",
                          "fysiskDokument": "${Base64.encode(forventetDokument)}",
                          "variantformat": "ARKIV"
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()

        val responsJson =
                """
                {
                  "journalpostId": "JOURNALPOST_ID",
                  "journalpostferdigstilt": true,
                  "dokumenter": [
                    {
                      "dokumentInfoId": 123
                    }
                  ]
                }
            """.trimIndent()

        `when`(systemUserTokenProvider.getSystemUserToken()).thenReturn("SYSTEM_USER_TOKEN")

        givenThat(
                post(urlEqualTo("/rest/journalpostapi/v1/journalpost"))
                        .withRequestBody(equalToJson(forventetRequest))
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withBody(responsJson))
        )

        val respons =
                AuthContextHolder.withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "SUBJECT"), UnsafeSupplier {
                    dokumentService.journalforDokument(
                            tittel = "Tittel",
                            enhetId = EnhetId("ENHET_ID"),
                            fnr = Fnr("fnr"),
                            oppfolgingssak = "OPPF_SAK",
                            brevkode = "BREV_KODE",
                            dokument = forventetDokument
                    )
                })

        val forventetRespons = OpprettetJournalpostDTO(
                journalpostId = "JOURNALPOST_ID",
                journalpostferdigstilt = true,
                dokumenter = listOf(OpprettetJournalpostDTO.DokumentInfoId("123")))

        assertEquals(forventetRespons, respons)
    }
}

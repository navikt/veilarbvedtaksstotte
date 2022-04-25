package no.nav.veilarbvedtaksstotte.service

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.nimbusds.jose.util.Base64
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.sts.SystemUserTokenProvider
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClient
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClientImpl
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClientImpl
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettetJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokument.MalType
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2ClientImpl
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClientImpl
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClientImpl
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClientImpl
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClientImpl
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClientImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.*

class DokumentServiceTest {

    lateinit var veilarbarenaClient: VeilarbarenaClient
    lateinit var veilarbpersonClient: VeilarbpersonClient
    lateinit var veilarbveilederClient: VeilarbveilederClient
    lateinit var regoppslagClient: RegoppslagClient
    lateinit var dokarkivClient: DokarkivClient
    lateinit var veilarbregistreringClient: VeilarbregistreringClient
    lateinit var pdfClient: PdfClient
    lateinit var norg2Client: Norg2Client
    lateinit var enhetInfoService: EnhetInfoService
    lateinit var malTypeService: MalTypeService
    lateinit var dokumentService: DokumentService

    private val wireMockRule = WireMockRule()

    val systemUserTokenProvider: SystemUserTokenProvider = mock(SystemUserTokenProvider::class.java)

    @Rule
    fun getWireMockRule() = wireMockRule

    @Before
    fun setup() {
        val wiremockUrl = "http://localhost:" + getWireMockRule().port()
        regoppslagClient = RegoppslagClientImpl(wiremockUrl, systemUserTokenProvider)
        dokarkivClient =
            DokarkivClientImpl(wiremockUrl, systemUserTokenProvider, AuthContextHolderThreadLocal.instance())
        veilarbarenaClient = VeilarbarenaClientImpl(wiremockUrl, AuthContextHolderThreadLocal.instance())
        veilarbregistreringClient = VeilarbregistreringClientImpl(wiremockUrl,AuthContextHolderThreadLocal.instance())
        veilarbpersonClient = VeilarbpersonClientImpl(wiremockUrl, {""})
        veilarbveilederClient = VeilarbveilederClientImpl(wiremockUrl, AuthContextHolderThreadLocal.instance())
        pdfClient = PdfClientImpl(wiremockUrl)
        norg2Client = Norg2ClientImpl(wiremockUrl)
        enhetInfoService = EnhetInfoService(norg2Client)
        malTypeService = MalTypeService(veilarbregistreringClient)
        dokumentService = DokumentService(
            regoppslagClient = regoppslagClient,
            pdfClient = pdfClient,
            veilarbarenaClient = veilarbarenaClient,
            veilarbpersonClient = veilarbpersonClient,
            veilarbveilederClient = veilarbveilederClient,
            dokarkivClient = dokarkivClient,
            enhetInfoService = enhetInfoService,
            malTypeService = malTypeService
        )
    }


    @Test
    fun `journalforing av dokument gir forventet innhold i request og response`() {
        val forventetDokument = "dokument".toByteArray()
        val referanse = UUID.randomUUID()
        val forventetRequest =
            """
                {
                  "tittel": "Tittel",
                  "journalpostType": "UTGAAENDE",
                  "tema": "OPP",
                  "journalfoerendeEnhet": "ENHET_ID",
                  "eksternReferanseId": "$referanse",
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
                      "brevkode": "SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID",
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
            post(urlEqualTo("/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"))
                .withRequestBody(equalToJson(forventetRequest))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                        .withBody(responsJson)
                )
        )

        val respons =
            AuthContextHolderThreadLocal
                .instance()
                .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "SUBJECT"), UnsafeSupplier {
                    dokumentService.journalforDokument(
                        tittel = "Tittel",
                        enhetId = EnhetId("ENHET_ID"),
                        fnr = Fnr("fnr"),
                        oppfolgingssak = "OPPF_SAK",
                        malType = MalType.SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID,
                        dokument = forventetDokument,
                        referanse = referanse
                    )
                })

        val forventetRespons = OpprettetJournalpostDTO(
            journalpostId = "JOURNALPOST_ID",
            journalpostferdigstilt = true,
            dokumenter = listOf(OpprettetJournalpostDTO.DokumentInfoId("123"))
        )

        assertEquals(forventetRespons, respons)
    }
}

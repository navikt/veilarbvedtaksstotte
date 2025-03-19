package no.nav.veilarbvedtaksstotte.service

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.givenThat
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.nimbusds.jose.util.Base64
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.client.norg2.Enhet
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.veilarbvedtaksstotte.client.aiaBackend.AiaBackendClient
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.ArbeidssoekerRegisteretService
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.OppslagArbeidssoekerregisteretClientImpl
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClientImpl
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClientImpl
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.OpprettetJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokument.MalType
import no.nav.veilarbvedtaksstotte.client.dokument.ProduserDokumentDTO
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetKontaktinformasjon
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetOrganiserer
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetPostboksadresse
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2ClientImpl
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClient
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClientImpl
import no.nav.veilarbvedtaksstotte.client.person.BehandlingsNummer
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClientImpl
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClientImpl
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.SakDTO
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClientImpl
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.Veileder
import no.nav.veilarbvedtaksstotte.domain.Malform
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.TestUtils.givenWiremockOkJsonResponse
import no.nav.veilarbvedtaksstotte.utils.TestUtils.givenWiremockOkJsonResponseForPost
import no.nav.veilarbvedtaksstotte.utils.toJson
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.*

@WireMockTest
class DokumentServiceTest {

    lateinit var authContextHolder: AuthContextHolder
    lateinit var veilarbarenaClient: VeilarbarenaClient
    lateinit var veilarboppfolgingClient: VeilarboppfolgingClient
    lateinit var veilarbpersonClient: VeilarbpersonClient
    lateinit var veilarbveilederClient: VeilarbveilederClient
    lateinit var regoppslagClient: RegoppslagClient
    lateinit var dokarkivClient: DokarkivClient
    lateinit var pdfClient: PdfClient
    lateinit var norg2Client: Norg2Client
    lateinit var enhetInfoService: EnhetInfoService
    lateinit var malTypeService: MalTypeService
    lateinit var oyeblikksbildeService: OyeblikksbildeService
    lateinit var dokumentService: DokumentService
    lateinit var pdfService: PdfService
    lateinit var oppslagArbeidssoekerregisteretClientImpl: OppslagArbeidssoekerregisteretClientImpl
    lateinit var arbeidssoekerRegisteretService: ArbeidssoekerRegisteretService

    val malform = Malform.NB
    val veilederNavn = "Navn Veileder"
    val enhetNavn = "Navn Enhet"
    val kontaktEnhetNavn = "Navn Kontaktenhet"
    val enhetId = EnhetId("3423")
    val kontaktEnhetId = EnhetId("001")
    val enhetPostadresse = EnhetPostboksadresse("", "", "", "")
    val telefonnummer = "00000000"
    val enhetKontaktinformasjon = EnhetKontaktinformasjon(kontaktEnhetId, enhetPostadresse, telefonnummer)
    val enhet = Enhet().setEnhetNr(enhetId.get()).setNavn(enhetNavn)
    val kontaktEnhet = Enhet().setEnhetNr(kontaktEnhetId.get()).setNavn(kontaktEnhetNavn)
    val brevdataOppslag = DokumentService.BrevdataOppslag(
        enhetKontaktinformasjon = enhetKontaktinformasjon,
        malform = malform,
        veilederNavn = veilederNavn,
        enhet = enhet,
        kontaktEnhet = kontaktEnhet
    )
    val forventetBrev = "brev".toByteArray()
    val behovsvurderingPdf = "behovsvurdering".toByteArray()
    val arbeidssoekerRegisteretPdf = "arbeidssokerRegistret".toByteArray()
    val cvPdf = "CV".toByteArray()

    val produserDokumentDTO = ProduserDokumentDTO(
        brukerFnr = Fnr("123"),
        navn = "Navn Navnesen",
        malType = MalType.STANDARD_INNSATS_BEHOLDE_ARBEID,
        enhetId = enhetId,
        veilederIdent = "123123",
        begrunnelse = "begrunnelse",
        opplysninger = listOf("Kilde1", "kilde2"),
        utkast = false,
        adresse = ProduserDokumentDTO.AdresseDTO(
            adresselinje1 = "Adresselinje 1",
            adresselinje2 = "Adresselinje 2",
            adresselinje3 = "Adresselinje 3",
            postnummer = "0000",
            poststed = "Sted",
            land = "Sverige"
        )
    )

    val eksternJournalpostReferanse = UUID.randomUUID()
    val forventetJournalpostRequest = """
                {
                  "tittel": "Tittel",
                  "journalpostType": "UTGAAENDE",
                  "tema": "OPP",
                  "journalfoerendeEnhet": "ENHET_ID",
                  "eksternReferanseId": "$eksternJournalpostReferanse",
                  "avsenderMottaker": {
                    "id": "fnr",
                    "idType": "FNR"
                  },
                  "bruker": {
                    "id": "fnr",
                    "idType": "FNR"
                  },
                  "sak": {
                    "fagsakId": "123456",
                    "fagsaksystem": "ARBEIDSOPPFOLGING",
                    "sakstype": "FAGSAK"
                  },
                  "dokumenter": [
                    {
                      "tittel": "Tittel",
                      "brevkode": "SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID",
                      "dokumentvarianter": [
                        {
                          "filtype": "PDFA",
                          "fysiskDokument": "${Base64.encode(forventetBrev)}",
                          "variantformat": "ARKIV"
                        }
                      ]
                    },
                    {
                      "tittel": "Det du fortalte oss da du ble registrert som arbeidssoker",
                      "brevkode": "ARBEIDSSOKERREGISTRET",
                      "dokumentvarianter": [
                        {
                          "filtype": "PDFA",
                          "fysiskDokument": "${Base64.encode(arbeidssoekerRegisteretPdf)}",
                          "variantformat": "ARKIV"
                        }
                      ]
                    },
                    {
                      "tittel": "CV-en/jobbønskene dine på nav.no",
                      "brevkode": "CV_OG_JOBBPROFIL",
                      "dokumentvarianter": [
                        {
                          "filtype": "PDFA",
                          "fysiskDokument": "${Base64.encode(cvPdf)}",
                          "variantformat": "ARKIV"
                        }
                      ]
                    },
                    {
                      "tittel": "Svarene dine om behov for veiledning",
                      "brevkode": "EGENVURDERING",
                      "dokumentvarianter": [
                        {
                          "filtype": "PDFA",
                          "fysiskDokument": "${Base64.encode(behovsvurderingPdf)}",
                          "variantformat": "ARKIV"
                        }
                      ]
                    }
                  ]
                }
            """

    val forventetJournalpostResponsJson = """
                {
                  "journalpostId": "JOURNALPOST_ID",
                  "journalpostferdigstilt": true,
                  "dokumenter": [
                    {
                      "dokumentInfoId": 123
                    }
                  ]
                }
            """

    private val forventetJournalpostRespons = OpprettetJournalpostDTO(
        journalpostId = "JOURNALPOST_ID",
        journalpostferdigstilt = true,
        dokumenter = listOf(OpprettetJournalpostDTO.DokumentInfoId("123"))
    )

    @BeforeEach
    fun setup(wireMockRuntimeInfo: WireMockRuntimeInfo) {

        val wiremockUrl = "http://localhost:" + wireMockRuntimeInfo.httpPort
        authContextHolder = AuthContextHolderThreadLocal.instance()
        regoppslagClient = RegoppslagClientImpl(wiremockUrl) { "SYSTEM_USER_TOKEN" }
        dokarkivClient = DokarkivClientImpl(wiremockUrl) { "" }
        veilarbarenaClient = VeilarbarenaClientImpl(wiremockUrl) { "" }
        veilarboppfolgingClient = mock(VeilarboppfolgingClient::class.java)
        veilarbpersonClient = VeilarbpersonClientImpl(wiremockUrl, {""}, {""})
        oppslagArbeidssoekerregisteretClientImpl = OppslagArbeidssoekerregisteretClientImpl(wiremockUrl, {""})
        arbeidssoekerRegisteretService = ArbeidssoekerRegisteretService(oppslagArbeidssoekerregisteretClientImpl)
        veilarbveilederClient = VeilarbveilederClientImpl(wiremockUrl, AuthContextHolderThreadLocal.instance(), {""}, {""})
        pdfClient = PdfClientImpl(wiremockUrl)
        norg2Client = Norg2ClientImpl(wiremockUrl)
        enhetInfoService = EnhetInfoService(norg2Client)
        malTypeService = MalTypeService(arbeidssoekerRegisteretService)

        val authService = mock(AuthService::class.java)
        val oyeblikksbildeRepository = mock(OyeblikksbildeRepository::class.java)
        val vedtaksstotteRepository = mock(VedtaksstotteRepository::class.java)
        val aiaBackendClient = mock(AiaBackendClient::class.java)
        oyeblikksbildeService = OyeblikksbildeService(
            authService,
            oyeblikksbildeRepository,
            vedtaksstotteRepository,
            veilarbpersonClient,
            aiaBackendClient,
            arbeidssoekerRegisteretService
        )

        pdfService = PdfService(
            pdfClient = pdfClient,
            enhetInfoService = enhetInfoService,
            veilarbveilederClient = veilarbveilederClient,
            veilarbpersonClient = veilarbpersonClient
        )

        dokumentService = DokumentService(
            regoppslagClient = regoppslagClient,
            veilarboppfolgingClient = veilarboppfolgingClient,
            veilarbpersonClient = veilarbpersonClient,
            dokarkivClient = dokarkivClient,
            malTypeService = malTypeService,
            oyeblikksbildeService = oyeblikksbildeService,
            pdfService = pdfService
        )

        givenWiremockOkJsonResponse(
            "/api/v1/enhet/${enhetId}/kontaktinformasjon", EnhetKontaktinformasjon(enhetId, null, null).toJson()
        )

        givenWiremockOkJsonResponse(
            "/api/v1/enhet/${kontaktEnhetId}/kontaktinformasjon", enhetKontaktinformasjon.toJson()
        )

        givenWiremockOkJsonResponseForPost(
            "/api/v3/person/hent-malform",
            equalToJson("{\"fnr\":\"123\", \"behandlingsnummer\": \"" + BehandlingsNummer.VEDTAKSTOTTE.value + "\"}"),
            VeilarbpersonClientImpl.MalformRespons(malform.name).toJson()
        )

        givenWiremockOkJsonResponse(
            "/api/veileder/${produserDokumentDTO.veilederIdent}",
            Veileder(produserDokumentDTO.veilederIdent, veilederNavn).toJson()
        )

        givenWiremockOkJsonResponseForPost(
            "/api/veileder/hent-navn",
            containing(produserDokumentDTO.veilederIdent),
            veilederNavn
        )

        givenWiremockOkJsonResponse(
            "/api/v1/enhet?enhetStatusListe=AKTIV", listOf(
                enhet, kontaktEnhet
            ).toJson()
        )

        givenThat(
            post(urlEqualTo("/api/v1/genpdf/vedtak14a/vedtak14a")).withRequestBody(
                equalToJson(
                    DokumentService.mapBrevdata(produserDokumentDTO, brevdataOppslag).toJson()
                )
            ).willReturn(
                aResponse().withStatus(201).withBody(forventetBrev)
            )
        )

        givenThat(
            post(urlEqualTo("/api/v1/genpdf/vedtak14a/oyeblikkbilde-behovsvurdering")).willReturn(
                aResponse().withStatus(201).withBody(behovsvurderingPdf)
            )
        )

        givenThat(
            post(urlEqualTo("/api/v1/genpdf/vedtak14a/oyeblikkbilde-cv")).willReturn(
                aResponse().withStatus(201).withBody(cvPdf)
            )
        )

        givenWiremockOkJsonResponse(
            "/api/v1/enhet/3423/organisering", """[
                {
                    "orgType": "EIER",
                    "fra": "${LocalDate.now().minusDays(1)}",
                    "til": "${LocalDate.now().plusDays(1)}",
                    "organiserer": ${EnhetOrganiserer(kontaktEnhetId, kontaktEnhetNavn).toJson()}
                }
            ]"""
        )
    }

    @Test
    fun `produserDokument genererer brev`() {
        val produserDokument =
            authContextHolder.withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "test"), UnsafeSupplier {
                pdfService.produserDokument(produserDokumentDTO)
            })

        Assertions.assertEquals(String(forventetBrev), String(produserDokument))
    }

    @Test
    fun `produserDokument feiler dersom navn for enhet mangler`() {
        givenWiremockOkJsonResponse(
            "/api/v1/enhet?enhetStatusListe=AKTIV", listOf(enhet.setNavn(null), kontaktEnhet).toJson()
        )

        val exception = Assertions.assertThrows(IllegalStateException::class.java) {
            authContextHolder.withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "test"), UnsafeSupplier {
                pdfService.produserDokument(produserDokumentDTO)
            })
        }

        Assertions.assertEquals("Manglende navn for enhet ${produserDokumentDTO.enhetId}", exception.message)
    }

    @Test
    fun `journalforing av dokument gir forventet innhold i request og response`() {
        givenThat(
            post(urlEqualTo("/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"))
                .withRequestBody(equalToJson(forventetJournalpostRequest))
                .willReturn(aResponse().withStatus(201).withBody(forventetJournalpostResponsJson))
        )

        val respons = journalførMedForventetRequest()

        Assertions.assertEquals(forventetJournalpostRespons, respons)
    }

    @Test
    fun `journalforing håndterer at vedtak allerede er journalført (409 CONFLICT) dersom respons inneholder journalpost`() {
        givenThat(
            post(urlEqualTo("/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"))
                .withRequestBody(equalToJson(forventetJournalpostRequest))
                .willReturn(
                    aResponse().withStatus(HttpStatus.CONFLICT.value()).withBody(forventetJournalpostResponsJson)
                )
        )

        val respons = journalførMedForventetRequest()

        Assertions.assertEquals(forventetJournalpostRespons, respons)
    }

    private fun journalførMedForventetRequest(): OpprettetJournalpostDTO {
        return AuthContextHolderThreadLocal.instance()
            .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "SUBJECT"), UnsafeSupplier {
                dokumentService.journalforDokument(
                    tittel = "Tittel",
                    enhetId = EnhetId("ENHET_ID"),
                    fnr = Fnr("fnr"),
                    oppfolgingssak = SakDTO(UUID.randomUUID(), 123456, "ARBEIDSOPPFOLGING", "OPP"),
                    malType = MalType.SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID,
                    dokument = forventetBrev,
                    oyeblikksbildeCVDokument = cvPdf,
                    oyeblikksbildeBehovsvurderingDokument = behovsvurderingPdf,
                    oyeblikksbildeArbeidssokerRegistretDokument = arbeidssoekerRegisteretPdf,
                    referanse = eksternJournalpostReferanse
                )
            })
    }
}

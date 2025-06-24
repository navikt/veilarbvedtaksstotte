package no.nav.veilarbvedtaksstotte.service

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.nimbusds.jose.util.Base64
import io.getunleash.DefaultUnleash
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
import no.nav.veilarbvedtaksstotte.client.norg2.*
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClient
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClientImpl
import no.nav.veilarbvedtaksstotte.client.person.BehandlingsNummer
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClientImpl
import no.nav.veilarbvedtaksstotte.client.person.dto.FodselsdatoOgAr
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
    val fodselsdatoOgAr = FodselsdatoOgAr(foedselsdato = LocalDate.of(1990, 1, 1), foedselsaar = 1990)
    val brevdataOppslag = DokumentService.BrevdataOppslag(
        enhetKontaktinformasjon = enhetKontaktinformasjon,
        malform = malform,
        veilederNavn = veilederNavn,
        enhet = enhet,
        kontaktEnhet = kontaktEnhet,
        fodselsdatoOgAr = fodselsdatoOgAr
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
        utkast = false
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
        veilarbpersonClient = VeilarbpersonClientImpl(wiremockUrl, { "" }, { "" })
        oppslagArbeidssoekerregisteretClientImpl = OppslagArbeidssoekerregisteretClientImpl(wiremockUrl, { "" })
        arbeidssoekerRegisteretService = ArbeidssoekerRegisteretService(oppslagArbeidssoekerregisteretClientImpl)
        veilarbveilederClient =
            VeilarbveilederClientImpl(wiremockUrl, AuthContextHolderThreadLocal.instance(), { "" }, { "" })
        pdfClient = PdfClientImpl(wiremockUrl)
        norg2Client = Norg2ClientImpl(wiremockUrl)
        enhetInfoService = EnhetInfoService(norg2Client)
        malTypeService = MalTypeService(arbeidssoekerRegisteretService)

        val authService = mock(AuthService::class.java)
        val oyeblikksbildeRepository = mock(OyeblikksbildeRepository::class.java)
        val vedtaksstotteRepository = mock(VedtaksstotteRepository::class.java)
        val aiaBackendClient = mock(AiaBackendClient::class.java)
        val unleashService: DefaultUnleash = mock(DefaultUnleash::class.java)
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
            veilarbpersonClient = veilarbpersonClient,
            unleashService = unleashService
        )

        dokumentService = DokumentService(
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

        givenWiremockOkJsonResponseForPost(
            "/api/v3/person/hent-foedselsdato",
            equalToJson("{\"fnr\":\"123\", \"behandlingsnummer\": \"" + BehandlingsNummer.VEDTAKSTOTTE.value + "\"}"),
            FodselsdatoOgAr(foedselsdato = LocalDate.of(1990, 1, 1), foedselsaar = 1990).toJson()
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

    @Test
    fun `sjekk om person har ungdomsgaranti når vi har fødselsdato`() {
        val now = LocalDate.now()

        val fodselsdatoForGammel = now.minusYears(40)
        val fodselsdatoForUng = now.minusYears(5)
        val fodselsdatoPasse = now.minusYears(25)
        val fodselsdato15MedBdIMorgen = now.plusDays(1).minusYears(16)
        val fodselsdato16BdIDag = now.minusYears(16)
        val fodselsdato30BdIDag = now.minusYears(30)

        fun mapTilFodselsdato(fodselsdato: LocalDate): FodselsdatoOgAr {
            return FodselsdatoOgAr(fodselsdato, fodselsdato.year)
        }

        val fodselsdatoForGammelHarIkkeUngdomsgaranti =
            DokumentService.erIAlderForUngdomsgaranti(mapTilFodselsdato(fodselsdatoForGammel))
        val fodselsdatoForUngHarIkkeUngdomsgaranti =
            DokumentService.erIAlderForUngdomsgaranti(mapTilFodselsdato(fodselsdatoForUng))
        val fodselsdatoPasseHarUngdomsgaranti =
            DokumentService.erIAlderForUngdomsgaranti(mapTilFodselsdato(fodselsdatoPasse))
        val fodselsdato15MedBdIMorgenHarIkkeUngdomsgaranti =
            DokumentService.erIAlderForUngdomsgaranti(mapTilFodselsdato(fodselsdato15MedBdIMorgen))
        val fodselsdato16BdIDagHarUngdomsgaranti =
            DokumentService.erIAlderForUngdomsgaranti(mapTilFodselsdato(fodselsdato16BdIDag))
        val fodselsdato30BdIDagHarUngdomsgaranti =
            DokumentService.erIAlderForUngdomsgaranti(mapTilFodselsdato(fodselsdato30BdIDag))

        Assertions.assertFalse(fodselsdatoForGammelHarIkkeUngdomsgaranti, "Personen skal ikke ha ungdomsgaranti")
        Assertions.assertFalse(fodselsdatoForUngHarIkkeUngdomsgaranti, "Personen skal ikke ha ungdomsgaranti")
        Assertions.assertTrue(fodselsdatoPasseHarUngdomsgaranti, "Personen skal ha ungdomsgaranti")
        Assertions.assertFalse(fodselsdato15MedBdIMorgenHarIkkeUngdomsgaranti, "Personen skal ikke ha ungdomsgaranti")
        Assertions.assertTrue(fodselsdato16BdIDagHarUngdomsgaranti, "Personen skal ha ungdomsgaranti")
        Assertions.assertFalse(fodselsdato30BdIDagHarUngdomsgaranti, "Personen skal ikke ha ungdomsgaranti")
    }

    @Test
    fun `sjekk om person har ungdomsgaranti når fødselsdato er null`() {
        val now = LocalDate.now()
        val alderBlir15Ar = now.minusYears(15).year
        val alderBlir16Ar = now.minusYears(16).year
        val alderBlir30Ar = now.minusYears(30).year
        val alderBlir31Ar = now.minusYears(31).year

        val harAlderBlir15ArUngdomsgaranti = DokumentService.erIAlderForUngdomsgaranti(FodselsdatoOgAr(null, alderBlir15Ar ))
        val harAlderBlir16ArUngdomsgaranti = DokumentService.erIAlderForUngdomsgaranti(FodselsdatoOgAr(null, alderBlir16Ar))
        val harAlderBlir30ArUngdomsgaranti = DokumentService.erIAlderForUngdomsgaranti(FodselsdatoOgAr(null, alderBlir30Ar))
        val harAlderBlir31ArUngdomsgaranti = DokumentService.erIAlderForUngdomsgaranti(FodselsdatoOgAr(null, alderBlir31Ar))

        Assertions.assertFalse(harAlderBlir15ArUngdomsgaranti, "Personen skal ikke ha ungdomsgaranti")
        Assertions.assertTrue(harAlderBlir16ArUngdomsgaranti, "Personen skal ha ungdomsgaranti")
        Assertions.assertTrue(harAlderBlir30ArUngdomsgaranti, "Personen skal ha ungdomsgaranti")
        Assertions.assertFalse(harAlderBlir31ArUngdomsgaranti, "Personen skal ikke ha ungdomsgaranti")

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

package no.nav.veilarbvedtaksstotte.service

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.sts.SystemUserTokenProvider
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClientImpl
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mockito.mock

class DistribusjonServiceV2Test : DatabaseTest() {


    lateinit var vedtaksstotteRepository: VedtaksstotteRepository
    lateinit var dokdistribusjonClient: DokdistribusjonClient
    lateinit var distribusjonServiceV2: DistribusjonServiceV2

    private val wireMockRule = WireMockRule()
    private val exceptionRule = ExpectedException.none();

    val metricsService: MetricsService = mock(MetricsService::class.java)
    val systemUserTokenProvider: SystemUserTokenProvider = mock(SystemUserTokenProvider::class.java)
    val serviceTokenSupplier: () -> String = { "" }

    @Rule
    fun getWireMockRule() = wireMockRule

    @Rule
    fun getExceptionRule() = exceptionRule

    @Before
    fun setup() {
        val wiremockUrl = "http://localhost:" + getWireMockRule().port()
        vedtaksstotteRepository = VedtaksstotteRepository(jdbcTemplate, transactor)
        dokdistribusjonClient = DokdistribusjonClientImpl(wiremockUrl, serviceTokenSupplier)
        distribusjonServiceV2 = DistribusjonServiceV2(
            vedtaksstotteRepository, dokdistribusjonClient, metricsService
        )
    }

    @Test
    fun `distribuering av journalpost gir forventet innhold i request og response`() {
        val forventetRequest =
            """
                {
                    "bestillendeFagsystem": "BD11",
                    "dokumentProdApp": "VEILARB_VEDTAK14A",
                    "journalpostId": "123"
                }
                """

        val responsJson =
            """
                {
                   "bestillingsId": "BESTILLINGS_ID"
                } 
                """

        givenThat(
            post(urlEqualTo("/rest/v1/distribuerjournalpost"))
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
                    distribusjonServiceV2.distribuerJournalpost("123")
                })

        assertEquals("BESTILLINGS_ID", respons.id)
    }

    @Test
    fun skal_feile_hvis_vedtak_mangler_journalpost() {
        getExceptionRule().expectMessage(
            "Kan ikke distribuere vedtak med id 123 som mangler journalpostId(null) og/eller dokumentinfoId(234)"
        )
        val vedtak = Vedtak()
        vedtak.id = 123
        vedtak.dokumentInfoId = "234"
        distribusjonServiceV2.validerVedtakForDistribusjon(vedtak)
    }

    @Test
    fun skal_feile_hvis_vedtak_mangler_dokument_info_id() {
        getExceptionRule().expectMessage(
            "Kan ikke distribuere vedtak med id 123 som mangler journalpostId(321) og/eller dokumentinfoId(null)"
        )
        val vedtak = Vedtak()
        vedtak.id = 123
        vedtak.journalpostId = "321"
        distribusjonServiceV2.validerVedtakForDistribusjon(vedtak)
    }

    @Test
    fun skal_feile_hvis_vedtak_mangler_journapost_og_dokument_info_id() {
        getExceptionRule().expectMessage(
            "Kan ikke distribuere vedtak med id 123 som mangler journalpostId(null) og/eller dokumentinfoId(null)"
        )
        val vedtak = Vedtak()
        vedtak.id = 123
        distribusjonServiceV2.validerVedtakForDistribusjon(vedtak)
    }

    @Test
    fun skal_feile_hvis_vedtak_allerede_er_distribuert_til_bruker() {
        getExceptionRule().expectMessage(
            "Kan ikke distribuere vedtak med id 123 som allerede har en dokumentbestillingId(456)"
        )
        val vedtak = Vedtak()
        vedtak.id = 123
        vedtak.journalpostId = "321"
        vedtak.dokumentInfoId = "234"
        vedtak.dokumentbestillingId = "456"
        distribusjonServiceV2.validerVedtakForDistribusjon(vedtak)
    }

    @Test
    fun fattVedtakV2__ferdigstiller_vedtak_og_sender_metrikk_for_manuell_retting_dersom_distribusjon_feiler() {
        /*gittVersjon2AvFattVedtak()
        gittUtkastKlarForUtsendelse()
        `when`(VedtakServiceTest.dokdistribusjonClient.distribuerJournalpost(any()))
            .thenThrow(RuntimeException())
        fattVedtak()
        assertSendtVedtak()
        verify(VedtakServiceTest.metricsService).rapporterFeilendeDistribusjonAvJournalpost()*/
    }

    @Test
    fun fattVedtak_v2_sender_ikke_mer_enn_en_gang() {
        /*`when`(VedtakServiceTest.dokdistribusjonClient.distribuerJournalpost(any()))
            .thenReturn(DistribuerJournalpostResponsDTO(TestData.TEST_DOKUMENT_BESTILLING_ID))
        `when`(VedtakServiceTest.dokarkivClient.opprettJournalpost(any()))
            .thenAnswer(AnswersWithDelay(
                10
            )  // Simulerer tregt API
            { invocation: InvocationOnMock? ->
                OpprettetJournalpostDTO(
                    TestData.TEST_JOURNALPOST_ID,
                    true,
                    List.of(DokumentInfoId(TestData.TEST_DOKUMENT_ID))
                )
            })
            .thenThrow(RuntimeException("Simulerer duplikatkontroll i dokarkiv"))
        withContext(UnsafeRunnable {
            gittTilgang()
            gittUtkastKlarForUtsendelse()
            gittVersjon2AvFattVedtak()
            val id = VedtakServiceTest.vedtaksstotteRepository.hentUtkast(TestData.TEST_AKTOR_ID).id
            val stream =
                Stream.of(
                    UnsafeSupplier<Future<*>> { sendVedtakAsynk(id) },
                    UnsafeSupplier<Future<*>> { sendVedtakAsynk(id) },
                    UnsafeSupplier<Future<*>> { sendVedtakAsynk(id) }
                ).parallel()
            stream.forEach { f: UnsafeSupplier<Future<*>> ->
                try {
                    f.get().get()
                } catch (ignored: Exception) {
                }
            }
            verify(VedtakServiceTest.dokdistribusjonClient, times(1))
                .distribuerJournalpost(any())
        })*/
    }

}

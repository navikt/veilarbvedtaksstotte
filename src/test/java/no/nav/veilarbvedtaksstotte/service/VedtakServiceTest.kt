package no.nav.veilarbvedtaksstotte.service

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.givenThat
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import io.getunleash.DefaultUnleash
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.norg2.Enhet
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.test.auth.AuthTestUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.utils.fn.UnsafeRunnable
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.api.ApiResult
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.ArbeidssoekerregisteretApiOppslagV2ClientImpl
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.EgenvurderingDialogTjenesteClientImpl
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClientImpl
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClientImpl
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient
import no.nav.veilarbvedtaksstotte.client.dokdistkanal.DokdistkanalClient
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetKontaktinformasjon
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetStedsadresse
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClientImpl
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClientImpl
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingPeriodeDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingStatusDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingsenhetDTO
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.SakDTO
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.Veileder
import no.nav.veilarbvedtaksstotte.controller.AuditlogService
import no.nav.veilarbvedtaksstotte.controller.UtkastController
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository
import no.nav.veilarbvedtaksstotte.repository.KilderRepository
import no.nav.veilarbvedtaksstotte.repository.MeldingRepository
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository
import no.nav.veilarbvedtaksstotte.repository.RetryVedtakdistribusjonRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_AKTOR_ID
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_DOKUMENT_ID
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_JOURNALPOST_ID
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSENHET_ID
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSENHET_NAVN
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT
import no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_NAVN
import no.nav.veilarbvedtaksstotte.utils.TestUtils.readTestResourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID

@WireMockTest
class VedtakServiceJournalforingTest : DatabaseTest() {

    @Test
    fun `fatter vedtak og journalforer oyeblikksbilder fra eksterne tjenester`(wireMock: WireMockRuntimeInfo) {
        DbTestUtils.cleanupDb(jdbcTemplate)

        // Bruk ekte repository og HTTP-klienter for lagring, PDF-generering og journalføring.
        // Tjenester utenfor denne flyten erstattes med mocker.
        val url = "http://localhost:${wireMock.httpPort}"
        val personClient = VeilarbpersonClientImpl(url, { "test-token" }, { "test-token" })
        val aktorOppslagClient = Mockito.mock(AktorOppslagClient::class.java)
        val veilarboppfolgingClient = Mockito.mock(VeilarboppfolgingClient::class.java)
        val poaoTilgangClient = Mockito.mock(PoaoTilgangClient::class.java)
        val authService = Mockito.spy(
            AuthService(
                aktorOppslagClient,
                OppfolgingsenhetService(veilarboppfolgingClient),
                AuthContextHolderThreadLocal.instance(),
                poaoTilgangClient
            )
        )
        val vedtakRepository = VedtaksstotteRepository(jdbcTemplate, transactor)
        val oyeblikksbildeTjeneste = OyeblikksbildeService(
            authService,
            OyeblikksbildeRepository(jdbcTemplate),
            vedtakRepository,
            personClient,
            ArbeidssoekerregisteretApiOppslagV2ClientImpl(url) { "test-token" },
            EgenvurderingDialogTjenesteClientImpl(url) { "test-token" }
        )
        val veilarbveilederClient = Mockito.mock(VeilarbveilederClient::class.java)
        val enhetInfoService = Mockito.mock(EnhetInfoService::class.java)
        val veilederService = Mockito.mock(VeilederService::class.java)
        val pdfService = PdfService(
            PdfClientImpl(url),
            veilarbveilederClient,
            enhetInfoService,
            personClient,
            Mockito.mock(DefaultUnleash::class.java)
        )
        val dokumentService = DokumentService(
            veilarboppfolgingClient,
            personClient,
            DokarkivClientImpl(url) { "test-token" },
            MalTypeService(personClient),
            oyeblikksbildeTjeneste,
            pdfService
        )
        val fatting = VedtakService(
            transactor,
            vedtakRepository,
            BeslutteroversiktRepository(jdbcTemplate),
            KilderRepository(jdbcTemplate),
            MeldingRepository(jdbcTemplate),
            SafClientImpl(url, { "test-token" }, { "test-token" }, authService),
            authService,
            oyeblikksbildeTjeneste,
            veilederService,
            Mockito.mock(VedtakHendelserService::class.java),
            dokumentService,
            DistribusjonService(
                vedtakRepository,
                RetryVedtakdistribusjonRepository(jdbcTemplate),
                Mockito.mock(DokdistribusjonClient::class.java),
                Mockito.mock(DokdistkanalClient::class.java)
            ),
            Mockito.mock(MetricsService::class.java),
            Mockito.mock(LeaderElectionClient::class.java),
            Mockito.mock(SakStatistikkService::class.java),
            aktorOppslagClient,
            veilarboppfolgingClient,
            Mockito.mock(Gjeldende14aVedtakService::class.java),
            Mockito.mock(KafkaProducerService::class.java),
            Mockito.mock(DefaultUnleash::class.java)
        )

        // Gi testveilederen tilgang og oppfølgingsdata som kreves for å fatte vedtaket.
        Mockito.doReturn(TEST_VEILEDER_IDENT).`when`(authService).innloggetVeilederIdent
        Mockito.doReturn(UUID.randomUUID()).`when`(authService).hentInnloggetVeilederUUID()
        whenever(aktorOppslagClient.hentAktorId(TEST_FNR)).thenReturn(AktorId.of(TEST_AKTOR_ID))
        whenever(aktorOppslagClient.hentFnr(AktorId.of(TEST_AKTOR_ID))).thenReturn(TEST_FNR)
        whenever(poaoTilgangClient.evaluatePolicy(any()))
            .thenReturn(ApiResult.success(Decision.Permit))
        whenever(veilarboppfolgingClient.hentOppfolgingsenhet(TEST_FNR))
            .thenReturn(Optional.of(OppfolgingsenhetDTO(TEST_OPPFOLGINGSENHET_ID, "NAV Test")))
        whenever(veilarboppfolgingClient.erUnderOppfolging(TEST_FNR))
            .thenReturn(Optional.of(OppfolgingStatusDTO(true)))
        whenever(veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(TEST_FNR))
            .thenReturn(Optional.of(OppfolgingPeriodeDTO(UUID.randomUUID(), ZonedDateTime.now(), null)))
        whenever(veilarboppfolgingClient.hentOppfolgingsperiodeSak(any()))
            .thenReturn(SakDTO(UUID.randomUUID(), 12345, "ARBEIDSOPPFOLGING", "OPP"))
        whenever(veilederService.hentEnhetNavn(TEST_OPPFOLGINGSENHET_ID))
            .thenReturn(TEST_OPPFOLGINGSENHET_NAVN)
        whenever(veilederService.hentVeilederEllerNull(TEST_VEILEDER_IDENT))
            .thenReturn(Optional.of(Veileder(TEST_VEILEDER_IDENT, TEST_VEILEDER_NAVN)))
        whenever(veilarbveilederClient.hentVeilederNavn(TEST_VEILEDER_IDENT))
            .thenReturn(TEST_VEILEDER_NAVN)
        val enhetId = EnhetId.of(TEST_OPPFOLGINGSENHET_ID)
        whenever(enhetInfoService.utledEnhetKontaktinformasjon(enhetId))
            .thenReturn(
                EnhetKontaktinformasjon(enhetId, EnhetStedsadresse("", "", "", "", "", ""), "")
            )
        whenever(enhetInfoService.hentEnhet(enhetId))
            .thenReturn(Enhet().setNavn(TEST_OPPFOLGINGSENHET_NAVN))

        // Eksterne svar er kilden til øyeblikksbildene; testen legger dem ikke direkte i databasen.
        stubPost(
            "/api/v3/person/hent-cv_jobbprofil",
            readTestResourceFile("testdata/oyeblikksbilde-cv.json")
        )
        stubPost(
            "/api/v3/person/hent-siste-opplysninger-om-arbeidssoeker-med-profilering",
            readTestResourceFile("testdata/opplysningerOmArbeidssoekerMedProfilering.json")
        )
        stubPost("/api/v3/snapshot", readTestResourceFile("testdata/arbeidssoeker-egenvurdering.json"))
        stubPost("/api/v1/egenvurdering/dialog", """{"dialogId":123}""")
        stubPost(
            "/api/v3/person/hent-navn",
            """
                {"fornavn": "Fornavn", "etternavn": "Etternavn"}
            """.trimIndent()
        )
        stubPost("/api/v3/person/hent-malform", """{"malform": "NB"}""")
        stubPost(
            "/api/v3/person/hent-foedselsdato",
            """
                {"foedselsdato": "1990-03-12", "foedselsaar": 1990}
            """.trimIndent()
        )

        // PDF-tjenesten returnerer gjenkjennelig innhold for brevet og hvert øyeblikksbilde.
        stubPost("/api/v1/genpdf/vedtak14a/vedtak14a", "vedtaksbrev")
        stubPost("/api/v1/genpdf/vedtak14a/oyeblikksbilde-cv", "cv-pdf")
        stubPost("/api/v1/genpdf/vedtak14a/oyeblikksbilde-arbeidssokerregistret", "registrering-pdf")
        stubPost("/api/v1/genpdf/vedtak14a/oyeblikksbilde-behovsvurdering", "egenvurdering-pdf")

        // Dokarkiv oppretter journalposten, mens SAF returnerer dokument-ID for hvert vedlegg.
        givenThat(
            post(urlEqualTo("/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"))
                .willReturn(
                    aResponse().withStatus(201).withBody(
                        """
                            {
                              "journalpostId": "$TEST_JOURNALPOST_ID",
                              "journalpostferdigstilt": true,
                              "dokumenter": [{"dokumentInfoId": "$TEST_DOKUMENT_ID"}]
                            }
                        """.trimIndent()
                    )
                )
        )
        stubPost(
            "/graphql",
            """
                {
                  "data": {
                    "journalpost": {
                      "dokumenter": [
                        {"brevkode": "CV_OG_JOBBPROFIL", "dokumentInfoId": "cv-id"},
                        {"brevkode": "ARBEIDSSOKERREGISTRET", "dokumentInfoId": "registrering-id"},
                        {"brevkode": "EGENVURDERING_V2", "dokumentInfoId": "egenvurdering-id"}
                      ]
                    }
                  }
                }
            """.trimIndent()
        )

        val mvc = MockMvcBuilders.standaloneSetup(
            UtkastController(fatting, Mockito.mock(AuditlogService::class.java))
        ).build()

        // Send opplysningstekstene slik frontend sender dem, via HTTP-endepunktene for utkast og fatting.
        AuthContextHolderThreadLocal.instance().withContext(
            AuthTestUtils.createAuthContext(UserRole.INTERN, TEST_VEILEDER_IDENT),
            UnsafeRunnable {
                fatting.lagUtkast(TEST_FNR)
                val utkast = vedtakRepository.hentUtkast(TEST_AKTOR_ID)

                mvc.perform(
                    put("/api/utkast/{vedtakId}", utkast.id)
                        .contentType("application/json")
                        .content(
                            """
                                {
                                  "hovedmal": "SKAFFE_ARBEID",
                                  "begrunnelse": "En begrunnelse",
                                  "innsatsgruppe": "STANDARD_INNSATS",
                                  "opplysninger": [
                                    "CV-en/jobbønskene dine på nav.no",
                                    "Det du fortalte oss da du ble registrert som arbeidssøker",
                                    "Svarene dine om behov for veiledning"
                                  ]
                                }
                            """.trimIndent()
                        )
                ).andExpect(status().isOk)
                mvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/utkast/{vedtakId}/fattVedtak", utkast.id)
                ).andExpect(status().isOk)

                // Sjekk både at vedtaket er fattet, og at alle øyeblikksbilder har fått riktig dokument-ID.
                val fattetVedtak = vedtakRepository.hentFattedeVedtak(TEST_AKTOR_ID).single()
                assertEquals(VedtakStatus.SENDT, fattetVedtak.vedtakStatus)
                assertEquals(TEST_JOURNALPOST_ID, fattetVedtak.journalpostId)

                val oyeblikksbilder = oyeblikksbildeTjeneste.hentOyeblikksbildeForVedtak(fattetVedtak.id)
                assertThat(oyeblikksbilder.map { it.oyeblikksbildeType }).containsExactlyInAnyOrder(
                    OyeblikksbildeType.CV_OG_JOBBPROFIL,
                    OyeblikksbildeType.ARBEIDSSOKERREGISTRET,
                    OyeblikksbildeType.EGENVURDERING_V2
                )
                assertThat(oyeblikksbilder).allMatch { it.isJournalfort }
                assertEquals(
                    "cv-id",
                    oyeblikksbildeTjeneste.hentJournalfortDokumentId(fattetVedtak.id, OyeblikksbildeType.CV_OG_JOBBPROFIL)
                )
                assertEquals(
                    "registrering-id",
                    oyeblikksbildeTjeneste.hentJournalfortDokumentId(fattetVedtak.id, OyeblikksbildeType.ARBEIDSSOKERREGISTRET)
                )
                assertEquals(
                    "egenvurdering-id",
                    oyeblikksbildeTjeneste.hentJournalfortDokumentId(fattetVedtak.id, OyeblikksbildeType.EGENVURDERING_V2)
                )
            }
        )

        // Kontroller at vedleggene faktisk ble generert og sendt til Dokarkiv, ikke bare lagret lokalt.
        verify(postRequestedFor(urlEqualTo("/api/v1/genpdf/vedtak14a/oyeblikksbilde-cv")))
        verify(postRequestedFor(urlEqualTo("/api/v1/genpdf/vedtak14a/oyeblikksbilde-arbeidssokerregistret")))
        verify(postRequestedFor(urlEqualTo("/api/v1/genpdf/vedtak14a/oyeblikksbilde-behovsvurdering")))
        verify(
            postRequestedFor(urlEqualTo("/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"))
                .withRequestBody(
                    matchingJsonPath(
                        "$.dokumenter[?(@.brevkode == 'CV_OG_JOBBPROFIL')].dokumentvarianter[0].fysiskDokument",
                        equalTo("Y3YtcGRm")
                    )
                )
                .withRequestBody(matchingJsonPath("$.dokumenter[?(@.brevkode == 'ARBEIDSSOKERREGISTRET')]"))
                .withRequestBody(matchingJsonPath("$.dokumenter[?(@.brevkode == 'EGENVURDERING_V2')]"))
        )
    }

    private fun stubPost(path: String, body: String) {
        givenThat(post(urlEqualTo(path)).willReturn(aResponse().withStatus(200).withBody(body)))
    }
}

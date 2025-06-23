package no.nav.veilarbvedtaksstotte.service

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.givenThat
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClientImpl
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.dto.DistribuerJournalpostResponsDTO
import no.nav.veilarbvedtaksstotte.client.dokdistkanal.DokdistkanalClient
import no.nav.veilarbvedtaksstotte.client.dokdistkanal.DokdistkanalClientImpl
import no.nav.veilarbvedtaksstotte.domain.DistribusjonBestillingId
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.TestUtils.assertThrowsWithMessage
import no.nav.veilarbvedtaksstotte.utils.toJson
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

@WireMockTest
class DistribusjonServiceTest : DatabaseTest() {

    companion object {
        lateinit var wiremockUrl: String
        lateinit var vedtakRepository: VedtaksstotteRepository
        lateinit var dokdistribusjonClient: DokdistribusjonClient
        lateinit var dokdistkanalClient: DokdistkanalClient
        lateinit var distribusjonService: DistribusjonService

        @BeforeAll
        @JvmStatic
        fun setup(wireMockRuntimeInfo: WireMockRuntimeInfo) {
            wiremockUrl = "http://localhost:" + wireMockRuntimeInfo.httpPort
            vedtakRepository = VedtaksstotteRepository(jdbcTemplate, transactor)
            dokdistribusjonClient = DokdistribusjonClientImpl(wiremockUrl) { "" }
            dokdistkanalClient = DokdistkanalClientImpl(wiremockUrl) { "" }
            distribusjonService = DistribusjonService(
                vedtakRepository, dokdistribusjonClient, dokdistkanalClient
            )
        }
    }

    @Test
    fun `distribuerer vedtak`() {
        val vedtakId = gittFattetVedtakDer()
        val forventetBestillingId = gittOkResponseFraDistribuerjournalpost().bestillingsId

        distribusjonService.distribuerVedtak(vedtakId)

        val vedtak = vedtakRepository.hentVedtak(vedtakId)

        assertEquals(forventetBestillingId, vedtak.dokumentbestillingId)
        assertFalse(vedtak.isSender)
    }

    @Test
    fun `feiler dersom det forsøkes å distribuere et distribuert vedtak`() {
        val vedtakId = gittFattetVedtakDer()
        val bestillingsId = gittOkResponseFraDistribuerjournalpost().bestillingsId

        distribusjonService.distribuerVedtak(vedtakId)
        assertThrowsWithMessage<IllegalStateException>(
            "Kan ikke distribuere vedtak med id $vedtakId som allerede har en dokumentbestillingId($bestillingsId)",
        ) {
            distribusjonService.distribuerVedtak(vedtakId)
        }
    }

    @Test
    fun `validering skal feile hvis vedtak mangler journalpost`() {
        val vedtak = Vedtak()
        vedtak.id = 123
        vedtak.dokumentInfoId = "234"

        assertThrowsWithMessage<IllegalStateException>(
            "Kan ikke distribuere vedtak med id 123 som mangler journalpostId(null) og/eller dokumentinfoId(234)"
        ) {
            distribusjonService.validerVedtakForDistribusjon(vedtak)
        }
    }

    @Test
    fun `validering skal feile hvis vedtak mangler dokument info id`() {
        val vedtak = Vedtak()
        vedtak.id = 123
        vedtak.journalpostId = "321"

        assertThrowsWithMessage<IllegalStateException>(
            "Kan ikke distribuere vedtak med id 123 som mangler journalpostId(321) og/eller dokumentinfoId(null)"
        ) {
            distribusjonService.validerVedtakForDistribusjon(vedtak)
        }
    }

    @Test
    fun `validering skal feile hvis vedtak mangler journapost og dokument info id`() {
        val vedtak = Vedtak()
        vedtak.id = 123

        assertThrowsWithMessage<IllegalStateException>(
            "Kan ikke distribuere vedtak med id 123 som mangler journalpostId(null) og/eller dokumentinfoId(null)"
        ) {
            distribusjonService.validerVedtakForDistribusjon(vedtak)
        }
    }

    @Test
    fun `validering skal feile hvis vedtaket har dokumentbestillingId`() {
        val vedtak = Vedtak()
        vedtak.id = 123
        vedtak.journalpostId = "321"
        vedtak.dokumentInfoId = "234"
        vedtak.dokumentbestillingId = "456"

        assertThrowsWithMessage<IllegalStateException>(
            "Kan ikke distribuere vedtak med id 123 som allerede har en dokumentbestillingId(456)"
        ) {
            distribusjonService.validerVedtakForDistribusjon(vedtak)
        }
    }

    @Test
    fun `dersom dokdist feiler med statuskode 409 og bestillingsId i respons fordi vedtaket allerede er distribuert, så lagres bestillingsId`() {
        val vedtakId = gittFattetVedtakDer()

        val respons = DistribuerJournalpostResponsDTO(UUID.randomUUID().toString())
        gittResponseFraDistribuerjournalpost(respons = respons, status = HttpStatus.CONFLICT.value())

        val forventetBestillingId = respons.bestillingsId

        distribusjonService.distribuerVedtak(vedtakId)

        val vedtak = vedtakRepository.hentVedtak(vedtakId)

        assertEquals(forventetBestillingId, vedtak.dokumentbestillingId)
        assertFalse(vedtak.isSender)
    }

    @Test
    fun `dersom dokdist feiler med statuskode ulik 2xx så kastes exception og distribusjon kan forsøkes på nytt`() {

        val utvalgAvUventedeStatuser = listOf(300, 400, 401, 403, 404, 500, 503)
        val vedtakId = gittFattetVedtakDer()

        utvalgAvUventedeStatuser.forEach { status ->

            gittResponseFraDistribuerjournalpost(
                respons = DistribuerJournalpostResponsDTO(UUID.randomUUID().toString()),
                status = status
            )

            assertThrowsWithMessage<RuntimeException>(
                "Uventet status $status ved kall mot mot $wiremockUrl/rest/v1/distribuerjournalpost"
            ) {
                distribusjonService.distribuerVedtak(vedtakId)
            }

            val vedtakEtterFeiletForsøk = vedtakRepository.hentVedtak(vedtakId)
            assertFalse(vedtakEtterFeiletForsøk.isSender)
            assertNull(vedtakEtterFeiletForsøk.dokumentbestillingId)
        }

        gittOkResponseFraDistribuerjournalpost()
        distribusjonService.distribuerVedtak(vedtakId)

        val vedtakEtterVellykketForsøk = vedtakRepository.hentVedtak(vedtakId)

        assertFalse(vedtakEtterVellykketForsøk.isSender)
        assertNotNull(vedtakEtterVellykketForsøk.dokumentbestillingId)
    }

    @Test
    fun `dersom dokdist returnerer med statuskode 2xx med manglende eller uventet respons så markeres distribusjon som feilet og kan ikke forsøkes på nytt`() {
        val utvalgAvUventetRespons = listOf(null, "null", """{"bestillingsId": null}""", """{"feilFelt": "verdi"}""")

        utvalgAvUventetRespons.forEach { respons ->
            val vedtakId = gittFattetVedtakDer()

            gittResponseFraDistribuerjournalpost(respons = respons, status = 201)

            distribusjonService.distribuerVedtak(vedtakId)
            val vedtakMedFeiletDistribusjon = vedtakRepository.hentVedtak(vedtakId)

            assertEquals(DistribusjonBestillingId.Feilet.id, vedtakMedFeiletDistribusjon.dokumentbestillingId)
            assertFalse(vedtakMedFeiletDistribusjon.isSender)

            assertThrowsWithMessage<IllegalStateException>(
                "Kan ikke distribuere vedtak med id $vedtakId som allerede har en dokumentbestillingId(${DistribusjonBestillingId.Feilet.id})"
            ) {
                distribusjonService.distribuerVedtak(vedtakId)
            }
        }
    }

    @Test
    fun `vedtak distribueres ikke mer enn en gang selv om det gjøres flere samtidige kall`() {
        val vedtakId = gittFattetVedtakDer()
        val antallForsøk = 5
        gittOkResponseFraDistribuerjournalpost(delay = 2000)

        val countDownLatch = CountDownLatch(antallForsøk)

        List(antallForsøk) {
            { distribuerVedtakAsynk(vedtakId) }
        }.parallelStream().forEach { f ->
            try {
                f()?.get()
            } catch (_: Exception) {
            } finally {
                countDownLatch.countDown()
            }
        }

        countDownLatch.await()

        verify(exactly(1), postRequestedFor(urlEqualTo("/rest/v1/distribuerjournalpost")))
    }

    private val executorService: ExecutorService = Executors.newFixedThreadPool(3)

    private fun distribuerVedtakAsynk(id: Long): Future<*>? {
        return executorService.submit {
            distribusjonService.distribuerVedtak(id)
        }
    }

    private fun gittFattetVedtakDer(
        aktorId: AktorId = AktorId(RandomStringUtils.randomNumeric(10)),
        vedtakFattetDato: LocalDateTime = LocalDateTime.now(),
        veilederIdent: String = RandomStringUtils.randomAlphabetic(1) + RandomStringUtils.randomNumeric(6),
        oppfolgingsenhet: String = RandomStringUtils.randomNumeric(4),
        journalpostId: String = RandomStringUtils.randomNumeric(10),
        dokumentId: String = RandomStringUtils.randomNumeric(9)
    ): Long {
        vedtakRepository.opprettUtkast(
            aktorId.get(),
            veilederIdent,
            oppfolgingsenhet
        )
        val vedtak = vedtakRepository.hentUtkast(aktorId.get())
        vedtakRepository.lagreJournalforingVedtak(vedtak.id, journalpostId, dokumentId)
        vedtakRepository.ferdigstillVedtak(vedtak.id)
        jdbcTemplate.update("UPDATE VEDTAK SET VEDTAK_FATTET = ? WHERE ID = ?", vedtakFattetDato, vedtak.id)

        return vedtak.id
    }

    private fun gittResponseFraDistribuerjournalpost(
        respons: String?,
        status: Int,
        delay: Int? = null
    ) {
        val responseBuilder = aResponse()
            .withStatus(status)
        if (respons != null) {
            responseBuilder.withBody(respons)
        }
        if (delay != null) {
            responseBuilder.withFixedDelay(delay)
        }

        givenThat(
            post(urlEqualTo("/rest/v1/distribuerjournalpost"))
                .willReturn(responseBuilder)
        )
    }

    private fun gittResponseFraDistribuerjournalpost(
        respons: DistribuerJournalpostResponsDTO,
        status: Int,
        delay: Int? = null
    ) {
        gittResponseFraDistribuerjournalpost(respons.toJson(), status, delay)
    }

    private fun gittOkResponseFraDistribuerjournalpost(delay: Int? = null): DistribuerJournalpostResponsDTO {
        val respons = DistribuerJournalpostResponsDTO(UUID.randomUUID().toString())
        gittResponseFraDistribuerjournalpost(respons = respons, status = 201, delay = delay)
        return respons
    }
}

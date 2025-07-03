package no.nav.veilarbvedtaksstotte.service

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import io.micrometer.core.instrument.DistributionSummary
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2ClientImpl
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClient
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClientImpl
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClientImpl
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClientImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@WireMockTest
class PdfServiceTest {
    lateinit var veilarbpersonClient: VeilarbpersonClient
    lateinit var veilarbveilederClient: VeilarbveilederClient
    lateinit var pdfClient: PdfClient
    lateinit var norg2Client: Norg2Client
    lateinit var enhetInfoService: EnhetInfoService
    lateinit var pdfService: PdfService

    @BeforeEach
    fun setup(wireMockRuntimeInfo: WireMockRuntimeInfo) {
        val wiremockUrl = "http://localhost:" + wireMockRuntimeInfo.httpPort
        veilarbpersonClient = VeilarbpersonClientImpl(wiremockUrl, { "" }, { "" })
        veilarbveilederClient =
            VeilarbveilederClientImpl(wiremockUrl, AuthContextHolderThreadLocal.instance(), { "" }, { "" })
        pdfClient = PdfClientImpl(wiremockUrl)
        norg2Client = Norg2ClientImpl(wiremockUrl)
        enhetInfoService = EnhetInfoService(norg2Client)

        val mockCounter = mock<DistributionSummary>()
        whenever(mockCounter.record(any())).then {}

        pdfService = PdfService(
            pdfClient = pdfClient,
            enhetInfoService = enhetInfoService,
            veilarbveilederClient = veilarbveilederClient,
            veilarbpersonClient = veilarbpersonClient,
            antallTegnFjernetVedVask = mockCounter
        )
    }


    @Test
    fun `ugyldige tegn skal bli fjernet fra tekstinput til pdfgen`() {
        val ugyldigInput0002 = "Hello\u0002World\nLine2\u0000"
        val forventet0002 = "HelloWorld\nLine2"
        val vasket0002 = pdfService.vaskStringForUgyldigeTegnOgTell(ugyldigInput0002)
        assertEquals(forventet0002, vasket0002)

        val ugyldigFEFF = "Hello\uFEFFWorld Line2\uFEFF test\uFEFF"
        val forventetVasketFEFF = "HelloWorld Line2 test"
        val vasketFEFF = pdfService.vaskStringForUgyldigeTegnOgTell(ugyldigFEFF)
        assertEquals(forventetVasketFEFF, vasketFEFF)
    }
}

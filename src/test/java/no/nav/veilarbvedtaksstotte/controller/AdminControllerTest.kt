package no.nav.veilarbvedtaksstotte.controller;

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.veilarbvedtaksstotte.controller.AdminController.POAO_ADMIN
import no.nav.veilarbvedtaksstotte.controller.dto.SlettVedtakRequest
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.KafkaRepubliseringService
import no.nav.veilarbvedtaksstotte.service.UtrullingService
import no.nav.veilarbvedtaksstotte.service.VedtakService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*

@WebMvcTest(AdminController::class)
class AdminControllerTest {

    @MockkBean
    lateinit var authService: AuthService

    @MockkBean
    lateinit var utrullingService: UtrullingService

    @MockkBean
    lateinit var kafkaRepubliseringService : KafkaRepubliseringService

    @MockkBean
    lateinit var vedtakService : VedtakService

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `Slett vedtak`() {
        val fnr = "12345678901"
        val journalpostId = "123456789"
        val ansvarligVeileder = "Z123456"
        val slettVedtakBestillingId = "FAGSYSTEM-123456789"

        val request = """
                {
                  "fnr": "$fnr",
                  "journalpostId": "$journalpostId",
                  "ansvarligVeileder": "$ansvarligVeileder",
                  "slettVedtakBestillingId": "$slettVedtakBestillingId"
                }
                
                """.trimIndent()

        every {
            authService.erInternBruker()
        } returns true

        every {
            authService.erInnloggetBrukerModiaAdmin()
        } returns Unit

        every {
            authService.innloggetVeilederIdent
        } returns "Z654321"

        every {
            authService.hentApplikasjonFraContex()
        } returns POAO_ADMIN

        every {
            vedtakService.slettVedtak(SlettVedtakRequest(journalpostId, Fnr(fnr), NavIdent(ansvarligVeileder), slettVedtakBestillingId), NavIdent.of("Z654321"))
        } returns Unit

        val response = mockMvc.perform(
            put("/api/admin/slett-vedtak")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(request)
        )
            .andReturn().response

        assertEquals(200, response.status.toLong())
    }
}
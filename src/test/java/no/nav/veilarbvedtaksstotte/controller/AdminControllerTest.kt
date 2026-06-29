package no.nav.veilarbvedtaksstotte.controller

import no.nav.veilarbvedtaksstotte.controller.AdminController.POAO_ADMIN
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.KafkaRepubliseringService
import no.nav.veilarbvedtaksstotte.service.VedtakService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@WebMvcTest(AdminController::class)
class AdminControllerTest {

    @MockitoBean
    lateinit var authService: AuthService

    @MockitoBean
    lateinit var kafkaRepubliseringService: KafkaRepubliseringService

    @MockitoBean
    lateinit var vedtakService: VedtakService

    @MockitoBean
    lateinit var vedtaksstotteRepository: VedtaksstotteRepository

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

        Mockito.`when`(authService.erInternBruker()).thenReturn(true)
        Mockito.`when`(authService.innloggetVeilederIdent).thenReturn("Z654321")
        Mockito.`when`(authService.hentApplikasjonFraContex()).thenReturn(POAO_ADMIN)

        val response = mockMvc.perform(
            put("/api/admin/slett-vedtak")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(request)
        )
            .andReturn().response

        assertEquals(200, response.status.toLong())
    }
}

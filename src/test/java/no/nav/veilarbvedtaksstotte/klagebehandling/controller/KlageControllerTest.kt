package no.nav.veilarbvedtaksstotte.klagebehandling.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.*
import no.nav.veilarbvedtaksstotte.klagebehandling.service.KlageService
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.AuthService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(KlageController::class)
class KlageControllerTest {

    @MockkBean
    lateinit var klageService: KlageService

    @MockkBean
    lateinit var authService: AuthService

    @MockkBean
    lateinit var vedtakRepository: VedtaksstotteRepository

    @Autowired
    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockkObject(KlageController.Companion)
        every { KlageController.validerMiljo() } returns Unit
        every { KlageController.hentAktorId(any(), any()) } returns mockk()
        every { KlageController.validerTilganger(any(), any(), any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(KlageController.Companion)
    }

    @Test
    fun `start klagebehandling skal kun godta riktig request body`() {
        every { klageService.startNyKlagebehandling(any()) } just Runs
        val goodRequest = """
            {
               "vedtakId" : 123456789,
               "fnr" : "11111111111",
               "veilederIdent" : "Z123456",
               "klagedato" : "2026-02-14",
               "klageJournalpostid" : "987654321"
            }
            """.trimIndent()
        val badRequest = """
            {
              "vedtakId" : ,
              "fnr" : "11111111111",
              "veilederIdentFeilNavn" : "Z123456",
              "klagedato" : "2026-02-14",
              "klageJournalpostid" : ""
            }
            """.trimIndent()


        mockMvc.perform(
            post("/api/klagebehandling/opprett-klage")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(goodRequest)
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/klagebehandling/opprett-klage")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(badRequest)
        ).andExpect(status().isBadRequest)

    }
}

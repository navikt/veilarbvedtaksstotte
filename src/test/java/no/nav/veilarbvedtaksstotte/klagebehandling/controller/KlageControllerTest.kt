package no.nav.veilarbvedtaksstotte.klagebehandling.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import no.nav.veilarbvedtaksstotte.klagebehandling.service.KlageService
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

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `start klagebehandling skal kun godta riktig request body`() {
        every { klageService.opprettKlageBehandling(any()) } just Runs
        val goodRequest = """
            {
               "vedtakId" : 123456789,
               "fnr" : "11111111111",
               "veilederIdent" : "Z123456"
            }
            """.trimIndent()
        val badRequest = """
            {
              "vedtakId" : ,
              "fnr" : "11111111111",
              "veilederIdentFeilNavn" : "Z123456"
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

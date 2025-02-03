package no.nav.veilarbvedtaksstotte.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.Siste14aVedtakService
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.web.server.ResponseStatusException

@WebMvcTest(Siste14aVedtakController::class)
@Import(AuthService::class)
class Siste14aVedtakControllerTest {

    @MockkBean
    lateinit var authService: AuthService

    @MockkBean
    lateinit var siste14aVedtakService: Siste14aVedtakService

    @Autowired
    lateinit var mockMvc: MockMvc

    val fnr = Fnr(RandomStringUtils.randomNumeric(11))

    @BeforeEach
    fun beforeEach() {
        every {
            siste14aVedtakService.siste14aVedtak(fnr)
        } returns null
    }

    @Test
    fun `gir tilgang til systembruker med rolle siste-14a-vedtak`() {

        every {
            authService.erSystemBruker()
        } returns true

        every {
            authService.harSystemTilSystemTilgangMedEkstraRolle("siste-14a-vedtak")
        } returns true

        val response = mockMvc.perform(MockMvcRequestBuilders.get("/api/siste-14a-vedtak").queryParam("fnr", fnr.get()))
            .andReturn().response

        assertEquals(200, response.status.toLong())
    }

    @Test
    fun `gir ikke tilgang til systembruker uten rolle siste-14a-vedtak`() {

        every {
            authService.erSystemBruker()
        } returns true

        every {
            authService.harSystemTilSystemTilgangMedEkstraRolle("siste-14a-vedtak")
        } returns false

        val response = mockMvc.perform(MockMvcRequestBuilders.get("/api/siste-14a-vedtak").queryParam("fnr", fnr.get()))
            .andReturn().response

        assertEquals(403, response.status.toLong())
    }

    @Test
    fun `gir tilgang hvis ikke systembruker og tilgang til bruker`() {

        every {
            authService.erSystemBruker()
        } returns false

        every {
            authService.sjekkVeilederTilgangTilBruker(fnr = fnr)
        } answers { }

        val response = mockMvc.perform(MockMvcRequestBuilders.get("/api/siste-14a-vedtak").queryParam("fnr", fnr.get()))
            .andReturn().response

        assertEquals(200, response.status.toLong())
    }

    @Test
    fun `gir ikke tilgang hvis ikke systembruker og ikke tilgang til bruker`() {

        every {
            authService.erSystemBruker()
        } returns false

        every {
            authService.sjekkVeilederTilgangTilBruker(fnr = fnr)
        } throws ResponseStatusException(HttpStatus.FORBIDDEN)

        val response = mockMvc.perform(MockMvcRequestBuilders.get("/api/siste-14a-vedtak").queryParam("fnr", fnr.get()))
            .andReturn().response

        assertEquals(403, response.status.toLong())
    }
}

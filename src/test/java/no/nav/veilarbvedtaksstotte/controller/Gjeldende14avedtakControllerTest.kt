package no.nav.veilarbvedtaksstotte.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.controller.v2.dto.Gjeldende14aVedtakRequest
import no.nav.veilarbvedtaksstotte.controller.v2.dto.Siste14aVedtakRequest
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.Gjeldende14aVedtakService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.web.server.ResponseStatusException

@WebMvcTest(Gjeldende14aVedtakController::class)
@Import(AuthService::class)
class Gjeldende14avedtakControllerTest {

    @MockkBean
    lateinit var authService: AuthService

    @MockkBean
    lateinit var gjeldende14aVedtakService: Gjeldende14aVedtakService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var mockMvc: MockMvc

    val fnr = Fnr("12345678192")

    @BeforeEach
    fun beforeEach() {
        every {
            gjeldende14aVedtakService.hentGjeldende14aVedtak(fnr)
        } returns null
    }

    @Test
    fun `gir tilgang til systembruker med rolle gjeldende-14a-vedtak`() {

        every {
            authService.erSystemBruker()
        } returns true

        every {
            authService.harSystemTilSystemTilgangMedEkstraRolle("gjeldende-14a-vedtak")
        } returns true

        val response = mockMvc.perform(
            post("/api/hent-gjeldende-14a-vedtak")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(Gjeldende14aVedtakRequest(fnr))))
            .andReturn().response

        assertEquals(200, response.status.toLong())
    }

    @Test
    fun `gir ikke tilgang til systembruker uten rolle gjeldende-14a-vedtak`() {

        every {
            authService.erSystemBruker()
        } returns true

        every {
            authService.harSystemTilSystemTilgangMedEkstraRolle("gjeldende-14a-vedtak")
        } returns false

        val response = mockMvc.perform(post("/api/hent-gjeldende-14a-vedtak")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(Siste14aVedtakRequest(fnr))))
            .andReturn().response

        assertEquals(403, response.status.toLong())
    }

    @Test
    fun `gir tilgang hvis ikke systembruker og tilgang til bruker`() {

        every {
            authService.erSystemBruker()
        } returns false

        every {
            authService.erEksternBruker()
        } returns false

        every {
            authService.sjekkVeilederTilgangTilBruker(tilgangType = TilgangType.LESE, fnr = fnr)
        } answers { }

        val response = mockMvc.perform(post("/api/hent-gjeldende-14a-vedtak")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(Siste14aVedtakRequest(fnr))))
            .andReturn().response

        assertEquals(200, response.status.toLong())
    }

    @Test
    fun `gir tilgang hvis ikke systembruker og tilgang til bruker i ekstern endepunkt`() {

        every {
            authService.erSystemBruker()
        } returns false

        every {
            authService.erEksternBruker()
        } returns false

        every {
            authService.sjekkVeilederUtenModiarolleTilgangTilBruker(fnr = fnr)
        } answers { }

        val response = mockMvc.perform(post("/api/ekstern/hent-gjeldende-14a-vedtak")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(Siste14aVedtakRequest(fnr))))
            .andReturn().response

        assertEquals(200, response.status.toLong())
    }

    @Test
    fun `gir ikke tilgang hvis ikke systembruker og ikke tilgang til bruker`() {

        every {
            authService.erSystemBruker()
        } returns false

        every {
            authService.erEksternBruker()
        } returns false

        every {
            authService.sjekkVeilederTilgangTilBruker(tilgangType = TilgangType.LESE, fnr = fnr)
        } throws ResponseStatusException(HttpStatus.FORBIDDEN)

        val response = mockMvc.perform(post("/api/hent-gjeldende-14a-vedtak")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(Siste14aVedtakRequest(fnr))))
            .andReturn().response

        assertEquals(403, response.status.toLong())
    }
}


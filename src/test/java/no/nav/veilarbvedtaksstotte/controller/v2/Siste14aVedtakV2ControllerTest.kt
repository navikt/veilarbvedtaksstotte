package no.nav.veilarbvedtaksstotte.controller.v2

import tools.jackson.databind.ObjectMapper
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.controller.AuditlogService
import no.nav.veilarbvedtaksstotte.controller.v2.dto.Siste14aVedtakRequest
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.Siste14aVedtakService
import no.nav.veilarbvedtaksstotte.utils.TestUtils.randomNumeric
import org.mockito.Mockito
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.web.server.ResponseStatusException


@WebMvcTest(Siste14aVedtakV2Controller::class)
@Import(AuthService::class)
class Siste14aVedtakV2ControllerTest {

    @MockitoBean
    lateinit var authService: AuthService

    @MockitoBean
    lateinit var siste14aVedtakService: Siste14aVedtakService

    @MockitoBean
    lateinit var auditlogService: AuditlogService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var mockMvc: MockMvc

    val fnr = Fnr(randomNumeric(11))

    @BeforeEach
    fun beforeEach() {
        Mockito.`when`(siste14aVedtakService.hentSiste14aVedtak(fnr)).thenReturn(null)
    }

    @Test
    fun `gir tilgang til systembruker med rolle siste-14a-vedtak`() {

        Mockito.`when`(authService.erSystemBruker()).thenReturn(true)
        Mockito.`when`(authService.harSystemTilSystemTilgangMedEkstraRolle("siste-14a-vedtak")).thenReturn(true)

        val response = mockMvc.perform(post("/api/v2/hent-siste-14a-vedtak")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(Siste14aVedtakRequest(fnr))))
            .andReturn().response

        assertEquals(200, response.status.toLong())
    }

    @Test
    fun `gir ikke tilgang til systembruker uten rolle siste-14a-vedtak`() {

        Mockito.`when`(authService.erSystemBruker()).thenReturn(true)
        Mockito.`when`(authService.harSystemTilSystemTilgangMedEkstraRolle("siste-14a-vedtak")).thenReturn(false)

        val response = mockMvc.perform(post("/api/v2/hent-siste-14a-vedtak")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(Siste14aVedtakRequest(fnr))))
            .andReturn().response

        assertEquals(403, response.status.toLong())
    }

    @Test
    fun `gir tilgang hvis ikke systembruker og tilgang til bruker`() {

        Mockito.`when`(authService.erSystemBruker()).thenReturn(false)
        Mockito.`when`(authService.erEksternBruker()).thenReturn(false)

        val response = mockMvc.perform(post("/api/v2/hent-siste-14a-vedtak")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(Siste14aVedtakRequest(fnr))))
            .andReturn().response

        assertEquals(200, response.status.toLong())
    }

    @Test
    fun `gir ikke tilgang hvis ikke systembruker og ikke tilgang til bruker`() {

        Mockito.`when`(authService.erSystemBruker()).thenReturn(false)
        Mockito.`when`(authService.erEksternBruker()).thenReturn(false)
        Mockito.doThrow(ResponseStatusException(HttpStatus.FORBIDDEN)).`when`(authService)
            .sjekkVeilederTilgangTilBruker(TilgangType.LESE, fnr)
        val response = mockMvc.perform(post("/api/v2/hent-siste-14a-vedtak")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(Siste14aVedtakRequest(fnr))))
            .andReturn().response

        assertEquals(403, response.status.toLong())
    }
}

package no.nav.veilarbvedtaksstotte.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.controller.dto.toGjeldende14aVedtakDto
import no.nav.veilarbvedtaksstotte.controller.v2.dto.Gjeldende14aVedtakRequest
import no.nav.veilarbvedtaksstotte.controller.v2.dto.Siste14aVedtakRequest
import no.nav.veilarbvedtaksstotte.domain.vedtak.Gjeldende14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
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
import java.time.ZonedDateTime

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


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
                .content(objectMapper.writeValueAsString(Gjeldende14aVedtakRequest(fnr)))
        )
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

        val response = mockMvc.perform(
            post("/api/hent-gjeldende-14a-vedtak")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(Siste14aVedtakRequest(fnr)))
        )
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

        val response = mockMvc.perform(
            post("/api/hent-gjeldende-14a-vedtak")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(Siste14aVedtakRequest(fnr)))
        )
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

        val response = mockMvc.perform(
            post("/api/ekstern/hent-gjeldende-14a-vedtak")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(Siste14aVedtakRequest(fnr)))
        )
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

        val response = mockMvc.perform(
            post("/api/hent-gjeldende-14a-vedtak")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(Siste14aVedtakRequest(fnr)))
        )
            .andReturn().response

        assertEquals(403, response.status.toLong())
    }

    @Test
    fun `returnerer vedtak hvis tilgang`() {
        // Given
        every {
            authService.erSystemBruker()
        } returns false

        every {
            authService.erEksternBruker()
        } returns false

        every {
            authService.sjekkVeilederTilgangTilBruker(tilgangType = TilgangType.LESE, fnr = fnr)
        } answers { }

        val gjeldende14aVedtak = Gjeldende14aVedtak(
            aktorId = AktorId.of("1111111111111"),
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
            fattetDato = ZonedDateTime.now()
        )

        every { gjeldende14aVedtakService.hentGjeldende14aVedtak(fnr) } answers {
            gjeldende14aVedtak
        }


        // Then
        val expectedContent = """
        {
            "innsatsgruppe": "${gjeldende14aVedtak.innsatsgruppe}",
            "hovedmal": "${gjeldende14aVedtak.hovedmal}",
            "fattetDato": "${gjeldende14aVedtak.fattetDato}"
        }
        """.trimIndent()

        mockMvc.perform(
            post("/api/hent-gjeldende-14a-vedtak")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(
                    """
                    {
                        "fnr": "12345678900"
                    }
                    """.trimMargin()
                )
        ).andExpect(status().is(200))
            .andExpect(content().json(expectedContent))

    }

    @Test
    fun `Testar mappar mellom Gjeldende14aVedtak og tilhøyrande Dto`() {
        // Given
        val gjeldende14aVedtak = Gjeldende14aVedtak(
            aktorId = AktorId.of("1111111111111"),
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
            fattetDato = ZonedDateTime.now()
        )

        // When
        val gjeldende14aVedtakDTO = gjeldende14aVedtak.toGjeldende14aVedtakDto();

        // Then
        assertEquals(gjeldende14aVedtak.hovedmal, gjeldende14aVedtakDTO.hovedmal)
        assertEquals(gjeldende14aVedtak.innsatsgruppe, gjeldende14aVedtakDTO.innsatsgruppe)
        assertEquals(gjeldende14aVedtak.fattetDato, gjeldende14aVedtakDTO.fattetDato)
    }
}


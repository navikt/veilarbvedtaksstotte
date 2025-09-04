package no.nav.veilarbvedtaksstotte.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.http.Response.response
import com.ninjasquad.springmockk.MockkBean
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.every
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.controller.dto.Gjeldende14aVedtakDto
import no.nav.veilarbvedtaksstotte.controller.dto.toGjeldende14aVedtakDto
import no.nav.veilarbvedtaksstotte.controller.v2.dto.Gjeldende14aVedtakRequest
import no.nav.veilarbvedtaksstotte.domain.vedtak.*
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.Gjeldende14aVedtakService
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.http.hasBody
import okhttp3.internal.http.promisesBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.isNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.server.ResponseStatusException
import java.time.ZoneId
import java.time.ZonedDateTime


@WebMvcTest(Gjeldende14aVedtakController::class)
@Import(AuthService::class)
class Gjeldende14avedtakControllerTest {

    @MockkBean
    lateinit var authService: AuthService

    @MockkBean
    lateinit var gjeldende14aVedtakService: Gjeldende14aVedtakService

    @MockkBean
    lateinit var auditlogService: AuditlogService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var mockMvc: MockMvc

    val fnr = Fnr("12345678192")
    val fattetdato = ZonedDateTime.of(2025, 3, 14, 15, 9, 26, 0 , ZoneId.systemDefault())
    val fattetdatoString = fattetdato.toStr().split("[")[0] // fjernar [Europe/Oslo] og [ETC/UTC] frå datoen i høvesvis lokal køyring og github actions

    @BeforeEach
    fun beforeEach() {
        every {
            gjeldende14aVedtakService.hentGjeldende14aVedtak(fnr)
        } returns null

        every { auditlogService.auditlog(any(), any()) } answers { }
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
                .content(objectMapper.writeValueAsString(Gjeldende14aVedtakRequest(fnr)))
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
                .content(objectMapper.writeValueAsString(Gjeldende14aVedtakRequest(fnr)))
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
                .content(objectMapper.writeValueAsString(Gjeldende14aVedtakRequest(fnr)))
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
                .content(objectMapper.writeValueAsString(Gjeldende14aVedtakRequest(fnr)))
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
            fattetDato = fattetdato
        )

        every { gjeldende14aVedtakService.hentGjeldende14aVedtak(fnr) } answers {
            gjeldende14aVedtak
        }


        // Then
        val expectedContent = """
        {
            "innsatsgruppe": "${gjeldende14aVedtak.innsatsgruppe.mapTilInnsatsgruppeV2()}",
            "hovedmal": "${gjeldende14aVedtak.hovedmal}",
            "fattetDato": "$fattetdatoString"
        }
        """.trimIndent()

        mockMvc.perform(
            post("/api/hent-gjeldende-14a-vedtak")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(
                    """
                    {
                        "fnr": "$fnr"
                    }
                    """.trimMargin()
                )
        ).andExpect(status().`is`(200))
            .andExpect(content().json(expectedContent))
    }

    @Test
    fun `returnerer null hvis tilgang, men ingen vedtak`() {
        // Given
        every {
            authService.erSystemBruker()
        } returns false

        every {
            authService.erEksternBruker()
        } returns false

        every {
            authService.sjekkVeilederTilgangTilBruker(tilgangType = TilgangType.LESE, fnr = fnr)
        } answers { null.toJson() }

        val result = mockMvc.perform(
            post("/api/hent-gjeldende-14a-vedtak")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(
                    """
                    {
                        "fnr": "$fnr"
                    }
                    """.trimMargin()
                )
        ).andExpect(status().`is`(200))
            .andExpect(header().doesNotExist("content-type"))
            .andReturn().response

        assertEquals(0, result.contentLength)
        assertEquals("", result.contentAsString)
    }

    @Test
    fun `Testar mappar mellom Gjeldende14aVedtak og tilhøyrande Dto`() {
        // Given
        val gjeldende14aVedtak = Gjeldende14aVedtak(
            aktorId = AktorId.of("1111111111111"),
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
            hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
            fattetDato = fattetdato
        )

        // When
        val gjeldende14aVedtakDTO = gjeldende14aVedtak.toGjeldende14aVedtakDto();

        // Then
        val forventetGjeldende14aVedtak = Gjeldende14aVedtakDto(
            innsatsgruppe = InnsatsgruppeV2.GODE_MULIGHETER,
            hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
            fattetDato = fattetdato
        )

        assertEquals(forventetGjeldende14aVedtak, gjeldende14aVedtakDTO)
    }
}


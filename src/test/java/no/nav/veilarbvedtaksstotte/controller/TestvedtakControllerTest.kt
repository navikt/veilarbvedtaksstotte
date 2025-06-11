package no.nav.veilarbvedtaksstotte.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.controller.dto.TestvedtakRequest
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.InnsatsgruppeV2
import no.nav.veilarbvedtaksstotte.service.AuthService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.time.LocalDateTime

@WebMvcTest(TestvedtakController::class)
@Import(AuthService::class)
class TestvedtakControllerTest {

    @MockkBean
    lateinit var authService: AuthService

    @MockkBean
    lateinit var aktorOppslagClient: AktorOppslagClient

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `Skal kunne opprette et § 14 a-vedtak på testperson`() {
        val fnr = Fnr.of("12345678901")
        every {
            authService.harSystemTilSystemTilgangMedEkstraRolle("fatt-14a-vedtak")
        } returns true

        every {
            aktorOppslagClient.hentAktorId(fnr)
        } returns AktorId.of("1234567890123")

        val response = mockMvc.perform(
            post("/api/v1/test/vedtak")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(TestvedtakRequest(
                fnr = fnr,
                innsatsgruppe = InnsatsgruppeV2.GODE_MULIGHETER,
                oppfolgingsEnhet = "1234",
                hovedmal = Hovedmal.SKAFFE_ARBEID,
                vedtakFattet = LocalDateTime.now().minusDays(1)
            ))))
            .andReturn().response

        assertEquals(200, response.status.toLong())
    }
}
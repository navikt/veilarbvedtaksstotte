package no.nav.veilarbvedtaksstotte.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarbvedtaksstotte.controller.dto.OpprettTestvedtakRequest
import no.nav.veilarbvedtaksstotte.controller.dto.TestvedtakRequest
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.InnsatsgruppeV2
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.mapTilInnsatsgruppe
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.TestvedtakService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.time.LocalDateTime
import java.util.*

@WebMvcTest(TestvedtakController::class)
@Import(AuthService::class)
class TestvedtakControllerTest {

    @MockkBean
    lateinit var authService: AuthService

    @MockkBean
    lateinit var aktorOppslagClient: AktorOppslagClient

    @MockkBean
    lateinit var testvedtakService: TestvedtakService

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper


    companion object {

        private var mock = Mockito.mockStatic(EnvironmentUtils::class.java)

        @BeforeAll
        @JvmStatic
        fun setup() {
            mock.`when`<Optional<Boolean>> { EnvironmentUtils.isDevelopment() }.thenReturn(Optional.of(true))
        }

        @AfterAll
        @JvmStatic
        fun closedown() {
            mock.close()
        }
    }

    @Test
    fun `Skal kunne opprette et § 14 a-vedtak pa testperson`() {
        val fnr = Fnr.of("12345678901")
        val aktorId = AktorId.of("1234567890123")

        every {
            authService.harSystemTilSystemTilgangMedEkstraRolle("fatt-14a-vedtak")
        } returns true

        every {
            aktorOppslagClient.hentAktorId(fnr)
        } returns aktorId

        every {
            testvedtakService.lagreTestvedtak(any(), any(), any())
        } returns Unit

        val response = mockMvc.perform(
            post("/api/v1/test/vedtak")
                .header("nav-consumer-id", "test-consumer-id")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(
                    objectMapper.writeValueAsString(
                        OpprettTestvedtakRequest(
                            fnr = fnr,
                            innsatsgruppe = InnsatsgruppeV2.GODE_MULIGHETER,
                            oppfolgingsEnhet = "1234",
                            hovedmal = Hovedmal.SKAFFE_ARBEID,
                            vedtakFattet = LocalDateTime.now().minusDays(1),
                            veilederIdent = "Z123455",
                            begrunnelse = "Testvedtak for § 14 a"
                        )
                    )
                )
        )
            .andReturn().response

        assertEquals(200, response.status.toLong())
    }

    @Test
    fun `Skal kunne hent et § 14 a-vedtak pa testperson`() {
        val fnr = Fnr.of("12345678901")
        val aktorId = AktorId.of("1234567890123")
        val vedtakFattet = LocalDateTime.now().minusDays(1)
        val vedtak = Vedtak()
            .settAktorId(aktorId.get())
            .settHovedmal(Hovedmal.SKAFFE_ARBEID)
            .settInnsatsgruppe(InnsatsgruppeV2.GODE_MULIGHETER.mapTilInnsatsgruppe())
            .settOppfolgingsenhetId("1234")
            .settUtkastOpprettet(vedtakFattet)
            .settVedtakFattet(vedtakFattet)
            .settUtkastSistOppdatert(vedtakFattet)
            .settVeilederIdent("Z123456")
            .settBegrunnelse("Testvedtak for § 14 a")

        every {
            authService.harSystemTilSystemTilgangMedEkstraRolle("fatt-14a-vedtak")
        } returns true

        every {
            aktorOppslagClient.hentAktorId(fnr)
        } returns aktorId

        every {
            testvedtakService.hentTestvedtak(aktorId)
        } returns vedtak

        val response = mockMvc.perform(
            post("/api/v1/test/vedtak/hent-vedtak")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(TestvedtakRequest(fnr = fnr)))
        )
            .andReturn().response

        assertEquals(200, response.status.toLong())
    }

    @Test
    fun `Skal kunne slette § 14 a-vedtak pa testperson`() {
        val fnr = Fnr.of("12345678901")
        val aktorId = AktorId.of("1234567890123")

        every {
            authService.harSystemTilSystemTilgangMedEkstraRolle("fatt-14a-vedtak")
        } returns true

        every {
            aktorOppslagClient.hentAktorId(fnr)
        } returns aktorId

        every {
            testvedtakService.slettTestvedtak(any())
        } returns Unit

        val response = mockMvc.perform(
            delete("/api/v1/test/vedtak")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(TestvedtakRequest(fnr = fnr)))
        )
            .andReturn().response

        assertEquals(200, response.status.toLong())
    }
}
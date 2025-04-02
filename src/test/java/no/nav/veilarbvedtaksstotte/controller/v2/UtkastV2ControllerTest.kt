package no.nav.veilarbvedtaksstotte.controller.v2

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.controller.AuditlogService
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.VedtakService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [UtkastV2Controller::class])
class UtkastV2ControllerTest {
    @MockkBean
    lateinit var authService: AuthService

    @MockkBean
    lateinit var vedtakService: VedtakService

    @MockkBean
    lateinit var auditlogService: AuditlogService

    @Autowired
    lateinit var mockMvc: MockMvc

    val fnr: Fnr = Fnr.of("11111111111")

    @Test
    fun `hent vedtaksutkast for bruker`() {
        every {
            authService.erSystemBruker()
        } returns false

        every {
            authService.erEksternBruker()
        } returns false

        every {
            authService.sjekkVeilederTilgangTilBruker(tilgangType = TilgangType.SKRIVE, fnr = fnr)
        } answers { }

        every {
            vedtakService.hentUtkast(fnr)
        } returns Vedtak()

        every { auditlogService.auditlog(any(), any()) } answers { }

        val request = """
            {
              "fnr" : "11111111111"
            }
            """.trimIndent()

        val expectedResponse = """
            {
              "id": 0,
              "hovedmal": null,
              "innsatsgruppe": null,
              "utkastSistOppdatert": null,
              "begrunnelse": null,
              "veilederIdent": null,
              "veilederNavn": null,
              "oppfolgingsenhetId": null,
              "oppfolgingsenhetNavn": null,
              "beslutterIdent": null,
              "beslutterNavn": null,
              "opplysninger": null,
              "beslutterProsessStatus": null,
            }
            """.trimIndent()

        mockMvc.perform(
            post("/api/v2/hent-utkast")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(request)
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedResponse))
    }

}

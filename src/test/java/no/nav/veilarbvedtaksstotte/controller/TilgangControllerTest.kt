package no.nav.veilarbvedtaksstotte.controller

import io.getunleash.DefaultUnleash
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarbvedtaksstotte.client.tilgangsmaskin.TilgangsmaskinClient
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.OppfolgingsenhetService
import no.nav.veilarbvedtaksstotte.utils.TILGANGSMASKIN_INTEGRASJON
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional

@WebMvcTest(
    controllers = [TilgangController::class],
    properties = ["app.env.tilgangsmaskinEnkelttilgangEnabled=true"]
)
class TilgangControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var tilgangsmaskinClient: TilgangsmaskinClient

    @MockitoBean
    lateinit var authService: AuthService

    @MockitoBean
    lateinit var oppfolgingsenhetService: OppfolgingsenhetService

    @MockitoBean
    lateinit var unleashService: DefaultUnleash

    @ParameterizedTest
    @ValueSource(strings = ["enkelttilgang"])
    fun `nye endepunkter er utilgjengelige med bryteren av`(endepunkt: String) {
        mockMvc.perform(
            post("/api/v1/tilgang/$endepunkt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"brukerId":"11111111111"}""")
        ).andExpect(status().isNotFound)

        verifyNoInteractions(authService, tilgangsmaskinClient, oppfolgingsenhetService)
    }

    @Test
    fun `registrerer enkelttilgang med bryteren pa`() {
        whenever(unleashService.isEnabled(TILGANGSMASKIN_INTEGRASJON)).thenReturn(true)
        whenever(authService.innloggetVeilederIdent).thenReturn("Z123456")

        mockMvc.perform(
            post("/api/v1/tilgang/enkelttilgang")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"brukerId":"11111111111"}""")
        ).andExpect(status().isNoContent)

        verify(authService).sjekkInternBruker()
        verify(authService).sjekkTilgangTilModia()
        verify(tilgangsmaskinClient).opprettEnkelttilgang(any(), any(), any(), any())
    }

    @Test
    fun `henter tilgang uten enhetskontroll i dev med bryteren pa`() {
        whenever(unleashService.isEnabled(TILGANGSMASKIN_INTEGRASJON)).thenReturn(true)
        mockStatic(EnvironmentUtils::class.java).use { miljo ->
            miljo.`when`<Optional<Boolean>> { EnvironmentUtils.isProduction() }.thenReturn(Optional.of(false))

            mockMvc.perform(
                post("/api/v1/tilgang/hent-tilgang")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"brukerId":"11111111111"}""")
            ).andExpect(status().isOk).andExpect(content().string("true"))
        }

        verify(authService).sjekkInternBruker()
        verify(authService).sjekkTilgangTilModia()
        verifyNoInteractions(oppfolgingsenhetService, tilgangsmaskinClient)
    }
}

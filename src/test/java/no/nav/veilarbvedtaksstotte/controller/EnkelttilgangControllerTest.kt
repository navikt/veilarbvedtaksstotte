package no.nav.veilarbvedtaksstotte.controller

import no.nav.common.types.identer.NorskIdent
import no.nav.veilarbvedtaksstotte.client.tilgangsmaskin.TilgangsmaskinClient
import no.nav.veilarbvedtaksstotte.service.AuthService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(EnkelttilgangController::class)
@TestPropertySource(properties = ["app.env.tilgangsmaskinEnkelttilgangEnabled=true"])
class EnkelttilgangControllerTest {

    @MockitoBean
    lateinit var tilgangsmaskinClient: TilgangsmaskinClient

    @MockitoBean
    lateinit var authService: AuthService

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `registrerer enkelttilgang for innlogget veileder`() {
        `when`(authService.innloggetVeilederIdent).thenReturn("Z123456")
        val gyldigTil = LocalDate.now()

        mockMvc.perform(
            post("/api/v1/enkelttilganger")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"brukerId":"12345678910"}""")
        )
            .andExpect(status().isNoContent)

        val rekkefolge = inOrder(authService, tilgangsmaskinClient)
        rekkefolge.verify(authService).sjekkTilgangTilModia()
        rekkefolge.verify(tilgangsmaskinClient).opprettEnkelttilgang(
            "Z123456",
            NorskIdent.of("12345678910"),
            "Arbeidsrettet oppfølging i veilarbvedtaksstotte",
            gyldigTil
        )
    }
}

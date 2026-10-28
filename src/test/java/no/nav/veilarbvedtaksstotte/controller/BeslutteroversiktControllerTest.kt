package no.nav.veilarbvedtaksstotte.controller

import io.getunleash.DefaultUnleash
import no.nav.common.types.identer.EnhetId
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSok
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSokFilter
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BrukereMedAntall
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.BeslutteroversiktService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(BeslutteroversiktController::class)
class BeslutteroversiktControllerTest {

    @MockitoBean
    lateinit var beslutteroversiktService: BeslutteroversiktService

    @MockitoBean
    lateinit var unleashService: DefaultUnleash

    @MockitoBean
    lateinit var authService: AuthService

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `sok med null i enheter gir 400 uten å kalle tjenesten`() {
        sok("""{"filter":{"enheter":["1234",null]}}""")
            .andExpect(status().isBadRequest)

        verifyNoInteractions(beslutteroversiktService)
    }

    @Test
    fun `sok med gyldige enheter sendes videre til tjenesten`() {
        whenever(beslutteroversiktService.hentBeslutteroversikt(any())).thenReturn(BrukereMedAntall(emptyList(), 0))

        sok("""{"filter":{"enheter":["1234"]}}""")
            .andExpect(status().isOk)

        verify(beslutteroversiktService).hentBeslutteroversikt(
            BeslutteroversiktSok(filter = BeslutteroversiktSokFilter(enheter = listOf(EnhetId.of("1234"))))
        )
    }

    private fun sok(json: String) = mockMvc.perform(
        post("/api/beslutteroversikt/sok")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
    )
}

package no.nav.veilarbvedtaksstotte.klagebehandling.controller

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarbvedtaksstotte.klagebehandling.service.KlageService
import no.nav.veilarbvedtaksstotte.klagebehandling.service.Ok
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.AuthService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(KlageController::class)
class KlageControllerTest {

    @MockitoBean
    lateinit var klageService: KlageService

    @MockitoBean
    lateinit var authService: AuthService

    @MockitoBean
    lateinit var vedtakRepository: VedtaksstotteRepository

    @MockitoBean
    lateinit var aktorOppslagClient: AktorOppslagClient

    @Autowired
    lateinit var mockMvc: MockMvc

    private lateinit var mockedKlageController: MockedStatic<KlageController>

    @BeforeEach
    fun setUp() {
        EnvironmentUtils.setProperty("NAIS_CLUSTER_NAME", "dev-gcp", EnvironmentUtils.Type.PUBLIC)
        mockedKlageController = Mockito.mockStatic(KlageController::class.java)
        Mockito.`when`(aktorOppslagClient.hentIdenter(any())).thenReturn(
            BrukerIdenter(Fnr.of("11111111111"), AktorId.of("1234567890123"), emptyList(), emptyList())
        )
    }

    @AfterEach
    fun tearDown() {
        mockedKlageController.close()
        System.clearProperty("NAIS_CLUSTER_NAME")
    }

    @Test
    fun `start klagebehandling skal kun godta riktig request body`() {
        Mockito.`when`(klageService.startNyKlagebehandling(any())).thenReturn(Ok(data = UUID.randomUUID()))
        val goodRequest = """
            {
               "vedtakId" : 123456789,
               "fnr" : "11111111111",
               "veilederIdent" : "Z123456",
               "klagedato" : "2026-02-14",
               "klageJournalpostid" : "987654321"
            }
            """.trimIndent()
        val badRequest = """
            {
              "vedtakId" : ,
              "fnr" : "11111111111",
              "veilederIdentFeilNavn" : "Z123456",
              "klagedato" : "2026-02-14",
              "klageJournalpostid" : ""
            }
            """.trimIndent()



        mockMvc.perform(
            post("/api/klagebehandling/opprett-klage")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(goodRequest)
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/klagebehandling/opprett-klage")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(badRequest)
        ).andExpect(status().isBadRequest)

    }
}

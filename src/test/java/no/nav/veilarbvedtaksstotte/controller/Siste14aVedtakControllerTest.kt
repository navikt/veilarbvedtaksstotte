package no.nav.veilarbvedtaksstotte.controller

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.service.AuthService
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

@WebMvcTest(Siste14aVedtakController::class)
class Siste14aVedtakControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    val fnr = Fnr(RandomStringUtils.randomNumeric(11))

    @Test
    fun `request mot siste-14a-vedtak skal gi HTTP 410 Gone`() {
        val response = mockMvc.perform(MockMvcRequestBuilders.get("/api/siste-14a-vedtak").queryParam("fnr", fnr.get()))
            .andReturn().response

        assertEquals(410, response.status.toLong())
    }
}

package no.nav.veilarbvedtaksstotte.domain.beslutteroversikt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

class EnhetMedNavnTest {
    @Test
    fun serialisererEnheterSomObjekterMedIdOgNavn() {
        val objectMapper = ObjectMapper()

        val json = objectMapper.writeValueAsString(listOf(EnhetMedNavn("1234", "Nav Eksempel")))

        assertEquals("[{\"enhetId\":\"1234\",\"enhetNavn\":\"Nav Eksempel\"}]", json)
    }

    @Test
    fun dataClassHarVerdibasertLikhet() {
        assertEquals(EnhetMedNavn("1234", "Nav Eksempel"), EnhetMedNavn("1234", "Nav Eksempel"))
    }
}

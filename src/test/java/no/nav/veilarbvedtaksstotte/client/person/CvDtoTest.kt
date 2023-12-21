package no.nav.veilarbvedtaksstotte.client.person

import no.nav.veilarbvedtaksstotte.client.person.dto.CvInnhold
import no.nav.veilarbvedtaksstotte.utils.JsonUtils
import no.nav.veilarbvedtaksstotte.utils.TestUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CvDtoTest {

    @Test
    fun testDeserialization() {
        val cvJobbprofilJson = TestUtils.readTestResourceFile("testdata/cv-jobbprofil.json")
        val cvDto = JsonUtils.objectMapper.readValue(cvJobbprofilJson, CvInnhold::class.java);

        Assertions.assertEquals(cvDto.arbeidserfaring?.size, 0)
        Assertions.assertEquals(cvDto.utdanning?.size, 1)
        Assertions.assertEquals(cvDto.forerkort?.size, 1)
        Assertions.assertEquals(cvDto.kurs?.size, 0)
        Assertions.assertEquals(cvDto.sprak?.size, 1)
    }
}
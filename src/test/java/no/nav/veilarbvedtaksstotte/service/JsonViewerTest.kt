package no.nav.veilarbvedtaksstotte.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.veilarbvedtaksstotte.utils.TestUtils.readTestResourceFile
import org.junit.jupiter.api.Test

class JsonViewerTest {
    @Test
    fun `cv json to html`() {
        val cvJson = readTestResourceFile("oyeblikksbilde-cv.json")
        val mapper = ObjectMapper()
        val jsonToHtml = JsonViewer.jsonToHtml(cvJson)
        System.out.println(mapper.writeValueAsString(hashMapOf("htmlView" to jsonToHtml)))
    }
}
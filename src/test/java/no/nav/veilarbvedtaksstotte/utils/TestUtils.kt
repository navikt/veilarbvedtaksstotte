package no.nav.veilarbvedtaksstotte.utils

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.junit.Assert
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

object TestUtils {

    @JvmStatic
    fun readTestResourceFile(fileName: String?): String {
        val fileUrl = TestUtils::class.java.classLoader.getResource(fileName)
        val resPath = Paths.get(fileUrl.toURI())
        return String(Files.readAllBytes(resPath), StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun givenWiremockOkJsonResponse(url: String, json: String) {
        WireMock.givenThat(
            WireMock.get(WireMock.urlEqualTo(url))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)
                )
        )
    }

    @JvmStatic
    fun givenWiremockOkJsonResponseForPost(url: String, requestBody: StringValuePattern, json: String) {
        WireMock.givenThat(
            WireMock.post(url)
                .withRequestBody(requestBody)
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)
                )
        )
    }

    fun verifiserAsynkront(timeout: Long, unit: TimeUnit, verifiser: Runnable) {
        val timeoutMillis = unit.toMillis(timeout)
        var prosessert = false
        var timedOut = false
        val start = System.currentTimeMillis()
        while (!prosessert) {
            try {
                Thread.sleep(10)
                val current = System.currentTimeMillis()
                timedOut = current - start > timeoutMillis
                verifiser.run()
                prosessert = true
            } catch (a: Throwable) {
                if (timedOut) {
                    throw a
                }
            }
        }
    }

    @JvmStatic
    fun randomAlphabetic(length: Int): String =
        (1..length)
            .map { ('A'..'Z') + ('a'..'z') }
            .flatten()
            .shuffled()
            .take(length)
            .joinToString("")

    @JvmStatic
    fun randomNumeric(length: Int): String =
        (1..length)
            .map { ('0'..'9').random() }
            .joinToString("")

    inline fun <reified T : Throwable> assertThrowsWithMessage(messsage: String, runnable: Runnable) {
        val exception = Assert.assertThrows(
            T::class.java
        ) {
            runnable.run()
        }
        Assert.assertEquals(messsage, exception.message)
    }
}

package no.nav.veilarbvedtaksstotte.utils

import lombok.SneakyThrows
import org.junit.Assert
import java.lang.Runnable
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

object TestUtils {

    @JvmStatic
    @SneakyThrows
    fun readTestResourceFile(fileName: String?): String {
        val fileUrl = TestUtils::class.java.classLoader.getResource(fileName)
        val resPath = Paths.get(fileUrl.toURI())
        return String(Files.readAllBytes(resPath), StandardCharsets.UTF_8)
    }

    @SneakyThrows
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

    inline fun <reified T : Throwable> assertThrowsWithMessage(messsage: String, runnable: Runnable) {
        val exception = Assert.assertThrows(
            T::class.java
        ) {
            runnable.run()
        }
        Assert.assertEquals(messsage, exception.message)
    }
}

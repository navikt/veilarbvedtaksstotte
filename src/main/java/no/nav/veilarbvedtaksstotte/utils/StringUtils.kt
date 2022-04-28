package no.nav.veilarbvedtaksstotte.utils

object StringUtils {
    fun splitNewline(s: String): List<String> {
        return s.split("\n")
    }
}

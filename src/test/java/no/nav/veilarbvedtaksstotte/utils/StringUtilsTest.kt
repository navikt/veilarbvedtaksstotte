package no.nav.veilarbvedtaksstotte.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class StringUtilsTest {

    @Test
    fun `splitter ved newline`() {
        val splitNewline = StringUtils.splitNewline("a\nb\n\nc\\nd")
        assertEquals(listOf("a", "b", "", "c\\nd"), splitNewline)
    }
}

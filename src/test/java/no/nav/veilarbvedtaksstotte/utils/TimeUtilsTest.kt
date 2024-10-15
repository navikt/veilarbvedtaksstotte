package no.nav.veilarbvedtaksstotte.utils

import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toLocalDate
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeUtilsTest {

    @Test
    fun `konverterer ZonedDateTime til LocalDateTime i gjeldende tidssone, uavhengig av opprinnelig tidssone`() {
        val now = LocalDateTime.now()
        val systemOffsetSeconds = ZonedDateTime.now().offset.totalSeconds.toLong()
        val toLocalDateTime = toLocalDateTime(
            ZonedDateTime.of(
                now.minusSeconds(systemOffsetSeconds).plusHours(10),
                ZoneId.of("+10")
            )
        )
        assertEquals(now, toLocalDateTime)
    }

    @Test
    fun `konverterer ZonedDateTime til LocalDate i gjeldende tidssone, uavhengig av opprinnelig tidssone`() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val toLocalDateTime = toLocalDate(
            ZonedDateTime.of(today, LocalTime.MIDNIGHT, ZoneId.of("+10"))
        )

        assertEquals(yesterday, toLocalDateTime)
    }
}

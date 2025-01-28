package no.nav.veilarbvedtaksstotte.utils

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object TimeUtils {
    @JvmStatic
    fun toLocalDateTime(zonedDateTime: ZonedDateTime): LocalDateTime {
        return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }

    @JvmStatic
    fun toLocalDate(zonedDateTime: ZonedDateTime): LocalDate {
        return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
    }

    @JvmStatic
    fun toZonedDateTime(localDateTime: LocalDateTime): ZonedDateTime {
        return ZonedDateTime.of(localDateTime, ZoneId.systemDefault())
    }

    @JvmStatic
    fun toInstant(localDateTime: LocalDateTime): Instant {
        return toZonedDateTime(localDateTime).toInstant()
    }

    @JvmStatic
    fun toTimestampOrNull(instant: Instant?): Timestamp? {
        if (instant == null) {
            return null
        }
        return Timestamp.from(instant)
    }

    @JvmStatic
    fun now(): LocalDateTime{
        return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
    }
}

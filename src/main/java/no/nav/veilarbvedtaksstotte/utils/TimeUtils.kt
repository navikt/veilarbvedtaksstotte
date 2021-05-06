package no.nav.veilarbvedtaksstotte.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

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
}

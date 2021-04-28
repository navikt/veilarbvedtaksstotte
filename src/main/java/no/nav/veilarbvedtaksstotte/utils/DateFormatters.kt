package no.nav.veilarbvedtaksstotte.utils

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

object DateFormatters {
    val ISO_LOCAL_DATE_TIME_WITHOUT_T = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .appendLiteral(' ')
        .append(DateTimeFormatter.ISO_LOCAL_TIME)
        .toFormatter()

    val ISO_LOCAL_DATE_MIDNIGHT = DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .appendLiteral(" 00:00:00")
        .toFormatter()
}

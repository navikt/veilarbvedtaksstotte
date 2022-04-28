package no.nav.veilarbvedtaksstotte.utils

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.*

object DateFormatters {
    val ISO_LOCAL_DATE_TIME_WITHOUT_T: DateTimeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .appendLiteral(' ')
        .append(DateTimeFormatter.ISO_LOCAL_TIME)
        .toFormatter()

    val ISO_LOCAL_DATE_MIDNIGHT: DateTimeFormatter = DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .appendLiteral(" 00:00:00")
        .toFormatter()

    val NORSK_DATE: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendLocalized(FormatStyle.LONG, null)
        .toFormatter(Locale("no", "NO"))
}

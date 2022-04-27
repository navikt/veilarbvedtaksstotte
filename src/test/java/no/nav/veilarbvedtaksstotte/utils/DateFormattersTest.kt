package no.nav.veilarbvedtaksstotte.utils

import no.nav.veilarbvedtaksstotte.utils.DateFormatters.NORSK_DATE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DateFormattersTest {

    @Test
    fun formaterer_dato_riktig_for_alle_mnd() {
        assertEquals("5. januar 2020", LocalDate.of(2020, 1, 5).format(NORSK_DATE))
        assertEquals("1. februar 2021", LocalDate.of(2021, 2, 1).format(NORSK_DATE))
        assertEquals("9. mars 2022", LocalDate.of(2022, 3, 9).format(NORSK_DATE))
        assertEquals("17. april 2023", LocalDate.of(2023, 4, 17).format(NORSK_DATE))
        assertEquals("23. mai 2016", LocalDate.of(2016, 5, 23).format(NORSK_DATE))
        assertEquals("4. juni 2017", LocalDate.of(2017, 6, 4).format(NORSK_DATE))
        assertEquals("19. juli 2018", LocalDate.of(2018, 7, 19).format(NORSK_DATE))
        assertEquals("28. august 2019", LocalDate.of(2019, 8, 28).format(NORSK_DATE))
        assertEquals("11. september 2024", LocalDate.of(2024, 9, 11).format(NORSK_DATE))
        assertEquals("31. oktober 2025", LocalDate.of(2025, 10, 31).format(NORSK_DATE))
        assertEquals("27. november 2026", LocalDate.of(2026, 11, 27).format(NORSK_DATE))
        assertEquals("15. desember 2027", LocalDate.of(2027, 12, 15).format(NORSK_DATE))
    }
}

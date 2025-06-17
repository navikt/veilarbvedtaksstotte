package no.nav.veilarbvedtaksstotte.client.person.dto

import java.time.LocalDate

data class FodselsdatoOgAr(
    val foedselsdato: LocalDate? = null,
    val foedselsaar: Int
)

package no.nav.veilarbvedtaksstotte.client.person.dto

import java.time.LocalDate

data class FoedselsdatoOgAar(
    val foedselsdato: LocalDate? = null,
    val foedselsaar: Int
)

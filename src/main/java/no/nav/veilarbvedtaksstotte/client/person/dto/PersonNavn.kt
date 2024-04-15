package no.nav.veilarbvedtaksstotte.client.person.dto

data class PersonNavn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val forkortetNavn: String?
)

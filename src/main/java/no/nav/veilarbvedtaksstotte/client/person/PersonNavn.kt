package no.nav.veilarbvedtaksstotte.client.person

data class PersonNavn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val sammensattNavn: String?
)

package no.nav.veilarbvedtaksstotte.client.person.dto

enum class Gradering {
    STRENGT_FORTROLIG,
    FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    UGRADERT
}

data class Adressebeskyttelse(
    val gradering: Gradering
)
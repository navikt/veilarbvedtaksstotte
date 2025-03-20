package no.nav.veilarbvedtaksstotte.domain

import no.nav.common.types.identer.Id

/**
 * Representerer mapping-nøkkelen vi bruker internt for å koble flere identer (AktørID, fødselsnummer, osv.) til en
 * og samme fysiske person. Typen er et alias for [String], for å gjøre den mer meningsbærende.
 */
typealias PersonNokkel = String

/**
 * FOLKEREGISTERIDENT = Fødselsnummer eller D-nummer
 * AKTORID = AktørID
 * NPID = Navs personidentifikator
 */
enum class Gruppe {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID,
    UKJENT
}

data class IdentDetaljer(
    val ident: Id,
    val historisk: Boolean,
    val gruppe: Gruppe
)

data class PersonMedIdenter(
    val personNokkel: PersonNokkel,
    val identDetaljer: IdentDetaljer
)
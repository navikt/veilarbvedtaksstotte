package no.nav.veilarbvedtaksstotte.client.veilarbportefolje.dto

import no.nav.common.types.identer.NorskIdent

data class EtiketterForBrukerDTO(
    val fnr: NorskIdent,
    val etiketter: Etiketter
)

data class Etiketter(
    val erDoed: Boolean,
    val egenAnsatt: Boolean,
    val adressebeskyttelse: String?,
    val kandidatForUtmelding: Boolean,
)

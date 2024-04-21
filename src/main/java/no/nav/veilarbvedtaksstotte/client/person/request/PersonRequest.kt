package no.nav.veilarbvedtaksstotte.client.person.request

import no.nav.common.types.identer.Fnr

data class PersonRequest(
    val fnr: Fnr,
    val behandlingsummer: String?
)

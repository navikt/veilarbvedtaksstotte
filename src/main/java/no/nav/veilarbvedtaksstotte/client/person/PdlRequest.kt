package no.nav.veilarbvedtaksstotte.client.person

import no.nav.common.types.identer.Fnr

data class PdlRequest(
    val fnr: Fnr,
    val behandlingsnummer: String?)


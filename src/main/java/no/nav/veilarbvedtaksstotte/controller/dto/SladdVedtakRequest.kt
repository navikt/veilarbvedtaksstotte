package no.nav.veilarbvedtaksstotte.controller.dto

import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent

data class SladdVedtakRequest(
    val journalpostId: String,
    val fnr: Fnr,
    val ansvarligVeileder: NavIdent,
    val sladdVedtakBestillingId: String,
)

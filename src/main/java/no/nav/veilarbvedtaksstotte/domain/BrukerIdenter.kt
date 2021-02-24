package no.nav.veilarbvedtaksstotte.domain

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr

data class BrukerIdenter(
    val fnr: Fnr,
    val aktorId: AktorId,
    val historiskeFnr: List<Fnr>
)

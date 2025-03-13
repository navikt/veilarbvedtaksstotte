package no.nav.veilarbvedtaksstotte.domain.oppfolgingsperiode

import no.nav.common.types.identer.AktorId
import java.time.ZonedDateTime
import java.util.UUID

data class SisteOppfolgingsperiode(
    val oppfolgingsperiodeId: UUID,
    val aktorId: AktorId,
    val startdato: ZonedDateTime,
    val sluttdato: ZonedDateTime?
)

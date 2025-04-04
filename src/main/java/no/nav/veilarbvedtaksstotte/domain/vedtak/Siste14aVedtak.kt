package no.nav.veilarbvedtaksstotte.domain.vedtak

import no.nav.common.types.identer.AktorId
import java.time.ZonedDateTime

data class Siste14aVedtak(
    val aktorId: AktorId,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: HovedmalMedOkeDeltakelse?,
    val fattetDato: ZonedDateTime,
    val fraArena: Boolean
) {

}

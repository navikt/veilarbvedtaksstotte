package no.nav.veilarbvedtaksstotte.domain.vedtak

import no.nav.common.types.identer.AktorId
import java.time.ZonedDateTime
import java.util.*

data class Siste14aVedtak(
    val aktorId: AktorId,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: HovedmalMedOkeDeltakelse?,
    val fattetDato: ZonedDateTime,
    val fraArena: Boolean,
    val vedtakId: VedtakId
) {
    sealed interface VedtakId
    data class VedtakIdArena(val id: Long) : VedtakId
    data class VedtakIdVedtaksstotte(val id: Long, val referanse: UUID) : VedtakId
}

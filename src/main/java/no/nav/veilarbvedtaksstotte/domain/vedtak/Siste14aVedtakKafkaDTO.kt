package no.nav.veilarbvedtaksstotte.domain.vedtak

import no.nav.common.types.identer.AktorId
import java.time.ZonedDateTime

data class Siste14aVedtakKafkaDTO(
    val aktorId: AktorId,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: HovedmalMedOkeDeltakelse?,
    val fattetDato: ZonedDateTime,
    val fraArena: Boolean
)

fun Siste14aVedtak.toSiste14aVedtakKafkaDTO(): Siste14aVedtakKafkaDTO {
    return Siste14aVedtakKafkaDTO(
        aktorId = this.aktorId,
        innsatsgruppe = this.innsatsgruppe,
        hovedmal = this.hovedmal,
        fattetDato = this.fattetDato,
        fraArena = this.fraArena
    )
}

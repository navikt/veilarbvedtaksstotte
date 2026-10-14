package no.nav.veilarbvedtaksstotte.domain.vedtak

import no.nav.common.types.identer.AktorId
import java.time.ZonedDateTime

data class Siste14aVedtak(
    val aktorId: AktorId,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: HovedmalMedOkeDeltakelse?,
    val fattetDato: ZonedDateTime,
    val fraArena: Boolean,
    val oppfolgingsenhetId: String?
)


fun Siste14aVedtak.toSiste14aVedtakKafkaDto(): Siste14aVedtakKafkaDTO {
    return Siste14aVedtakKafkaDTO(
        aktorId = aktorId,
        innsatsgruppe = innsatsgruppe,
        hovedmal = hovedmal,
        fattetDato = fattetDato,
        fraArena = fraArena,
    )
}

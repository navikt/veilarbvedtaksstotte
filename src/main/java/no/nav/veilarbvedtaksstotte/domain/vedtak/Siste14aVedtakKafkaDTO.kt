package no.nav.veilarbvedtaksstotte.domain.vedtak

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toZonedDateTime
import java.time.ZonedDateTime

data class Siste14aVedtakKafkaDTO(
    val aktorId: AktorId,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: HovedmalMedOkeDeltakelse?,
    val fattetDato: ZonedDateTime,
    val fraArena: Boolean
) {

}

fun Vedtak.toSiste14aVedtakKafkaDto(): Siste14aVedtakKafkaDTO {
    return Siste14aVedtakKafkaDTO(
        aktorId = AktorId(aktorId),
        innsatsgruppe = innsatsgruppe,
        hovedmal = HovedmalMedOkeDeltakelse.fraHovedmal(hovedmal),
        fattetDato = toZonedDateTime(vedtakFattet),
        fraArena = false
    )
}

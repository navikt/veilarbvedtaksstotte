package no.nav.veilarbvedtaksstotte.domain.vedtak

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toZonedDateTime
import java.time.ZonedDateTime

data class Siste14aVedtak(
    val aktorId: AktorId,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: HovedmalMedOkeDeltakelse?,
    val fattetDato: ZonedDateTime,
    val fraArena: Boolean
) {

}

fun Vedtak.toSiste14aVedtak(): Siste14aVedtak {
    return Siste14aVedtak(
        aktorId = AktorId(aktorId),
        innsatsgruppe = innsatsgruppe,
        hovedmal = HovedmalMedOkeDeltakelse.fraHovedmal(hovedmal),
        fattetDato = toZonedDateTime(vedtakFattet),
        fraArena =  false
    )
}
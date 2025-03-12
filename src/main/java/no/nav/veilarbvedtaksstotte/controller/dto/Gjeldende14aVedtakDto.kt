package no.nav.veilarbvedtaksstotte.controller.dto

import no.nav.veilarbvedtaksstotte.domain.vedtak.Gjeldende14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import java.time.ZonedDateTime

data class Gjeldende14aVedtakDto(
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: HovedmalMedOkeDeltakelse?,
    val fattetDato: ZonedDateTime,
)

fun Gjeldende14aVedtak.toGjeldende14aVedtakDto(): Gjeldende14aVedtakDto {
    return Gjeldende14aVedtakDto(
        innsatsgruppe = innsatsgruppe,
        hovedmal = hovedmal,
        fattetDato = fattetDato,
    )
}

package no.nav.veilarbvedtaksstotte.controller.dto

import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import java.time.ZonedDateTime

data class Siste14aVedtakDTO(
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: HovedmalMedOkeDeltakelse?,
    val fattetDato: ZonedDateTime,
    val fraArena: Boolean
) {
    companion object {
        fun fraSiste14aVedtak(siste14aVedtak: Siste14aVedtak): Siste14aVedtakDTO {
            return Siste14aVedtakDTO(
                innsatsgruppe = siste14aVedtak.innsatsgruppe,
                hovedmal = siste14aVedtak.hovedmal,
                fattetDato = siste14aVedtak.fattetDato,
                fraArena = siste14aVedtak.fraArena
            )
        }
    }
}

package no.nav.veilarbvedtaksstotte.controller.dto

import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe

data class Siste14aVedtakDTO(
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: Siste14aVedtak.HovedmalMedOkeDeltakelse?
) {
    companion object {
        fun fraSiste14aVedtak(siste14aVedtak: Siste14aVedtak): Siste14aVedtakDTO {
            return Siste14aVedtakDTO(
                innsatsgruppe = siste14aVedtak.innsatsgruppe,
                hovedmal = siste14aVedtak.hovedmal
            )
        }
    }
}

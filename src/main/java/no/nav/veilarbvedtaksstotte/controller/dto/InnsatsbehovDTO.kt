package no.nav.veilarbvedtaksstotte.controller.dto

import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe

data class InnsatsbehovDTO(
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: Innsatsbehov.HovedmalMedOkeDeltakelse?
) {
    companion object {
        fun fraInnsatsbehov(innsatsbehov: Innsatsbehov): InnsatsbehovDTO {
            return InnsatsbehovDTO(
                innsatsgruppe = innsatsbehov.innsatsgruppe,
                hovedmal = innsatsbehov.hovedmal
            )
        }
    }
}

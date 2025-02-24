package no.nav.veilarbvedtaksstotte.controller.v2.dto

import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.InnsatsgruppeV2

data class KodeverkV2DTO(
    val innsatsgrupper: List<InnsatsgruppeKodeverkDTO>,
    val hovedmal: List<HovedmalKodeverkDTO>
)

data class InnsatsgruppeKodeverkDTO(
    val kode: InnsatsgruppeV2,
    val beskrivelse: String
)

data class HovedmalKodeverkDTO(
    val kode: Hovedmal,
    val beskrivelse: String
)

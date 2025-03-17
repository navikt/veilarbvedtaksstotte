package no.nav.veilarbvedtaksstotte.controller.v2.dto

import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.InnsatsgruppeDetaljertV2.ArenaInnsatsgruppeKode
import no.nav.veilarbvedtaksstotte.domain.vedtak.InnsatsgruppeV2

data class KodeverkV2DTO(
    val innsatsgrupper: List<InnsatsgruppeKodeverkV2DTO>,
    val hovedmal: List<HovedmalKodeverkV2DTO>
)

data class InnsatsgruppeKodeverkV2DTO(
    val kode: InnsatsgruppeV2,
    val gammelKode: Innsatsgruppe,
    val arenaKode: ArenaInnsatsgruppeKode,
    val beskrivelse: String
)

data class HovedmalKodeverkV2DTO(
    val kode: Hovedmal,
    val beskrivelse: String
)

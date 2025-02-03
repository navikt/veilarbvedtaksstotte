package no.nav.veilarbvedtaksstotte.controller.dto

import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.InnsatsgruppeDetaljert.ArenaInnsatsgruppeKode

data class KodeverkDTO(
    val innsatsgrupper: List<InnsatsgruppeKodeverkDTO>,
    val hovedmal: List<HovedmalKodeverkDTO>
)

data class InnsatsgruppeKodeverkDTO(
    val kode: Innsatsgruppe,
    val beskrivelse: String,
    val arenakode: ArenaInnsatsgruppeKode
)

data class HovedmalKodeverkDTO(
    val kode: HovedmalMedOkeDeltakelse,
    val beskrivelse: String
)

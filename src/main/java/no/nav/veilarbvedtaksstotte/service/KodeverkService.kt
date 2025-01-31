package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.controller.dto.HovedmalKodeverkDTO
import no.nav.veilarbvedtaksstotte.controller.dto.InnsatsgruppeKodeverkDTO
import no.nav.veilarbvedtaksstotte.controller.dto.KodeverkDTO
import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalDetaljert
import no.nav.veilarbvedtaksstotte.domain.vedtak.InnsatsgruppeDetaljert
import org.springframework.stereotype.Service

@Service
class KodeverkService {
    public fun lagKodeverk(): KodeverkDTO {
        return KodeverkDTO(
            innsatsgruppe = InnsatsgruppeDetaljert.entries.map {
                InnsatsgruppeKodeverkDTO(
                    kode = it.kode,
                    beskrivelse = it.beskrivelse,
                    arenaKode = it.arenaKode
                )
            },
            hovedmal = HovedmalDetaljert.entries.map {
                HovedmalKodeverkDTO(
                    kode = it.kode,
                    beskrivelse = it.beskrivelse
                )
            }
        )
    }
}
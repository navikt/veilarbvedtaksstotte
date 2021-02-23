package no.nav.veilarbvedtaksstotte.domain.vedtak

import no.nav.common.types.identer.Fnr
import java.time.LocalDateTime

data class ArenaVedtak(val fnr: Fnr,
                       val innsatsgruppe: ArenaInnsatsgruppe,
                       val hovedmal: ArenaHovedmal?,
                       val fraDato: LocalDateTime,
                       val regUser: String) {
    enum class ArenaInnsatsgruppe {
        BATT, BFORM , IKVAL, VARIG;

        companion object {
            fun tilInnsatsgruppe(arenaInnsatsgruppe: ArenaInnsatsgruppe): Innsatsgruppe =
                when(arenaInnsatsgruppe) {
                    BATT -> Innsatsgruppe.SPESIELT_TILPASSET_INNSATS
                    BFORM -> Innsatsgruppe.SITUASJONSBESTEMT_INNSATS
                    IKVAL -> Innsatsgruppe.STANDARD_INNSATS
                    VARIG -> Innsatsgruppe.VARIG_TILPASSET_INNSATS
                }
        }

    }
    enum class ArenaHovedmal {
        SKAFFE_ARBEID, BEHOLDE_ARBEID, OKE_DELTAKELSE // TODO riktige verdier?
    }
}

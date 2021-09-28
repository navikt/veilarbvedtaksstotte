package no.nav.veilarbvedtaksstotte.domain.vedtak

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaHovedmal
import java.time.ZonedDateTime

data class Siste14aVedtak(
    val aktorId: AktorId,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: HovedmalMedOkeDeltakelse?,
    val fattetDato: ZonedDateTime,
    val fraArena: Boolean
) {

    // Må ha med OKE_DELTAKELSE for bakoverkompabilitet på vedtak fra Arena.
    enum class HovedmalMedOkeDeltakelse {
        SKAFFE_ARBEID, BEHOLDE_ARBEID, OKE_DELTAKELSE;

        companion object {
            @JvmStatic
            fun fraHovedmal(hovedmal: Hovedmal?): HovedmalMedOkeDeltakelse? =
                when (hovedmal) {
                    Hovedmal.SKAFFE_ARBEID -> SKAFFE_ARBEID
                    Hovedmal.BEHOLDE_ARBEID -> BEHOLDE_ARBEID
                    null -> null
                }

            @JvmStatic
            fun fraArenaHovedmal(arenaHovedmal: ArenaHovedmal?): HovedmalMedOkeDeltakelse? =
                when (arenaHovedmal) {
                    ArenaHovedmal.SKAFFEA -> SKAFFE_ARBEID
                    ArenaHovedmal.BEHOLDEA -> BEHOLDE_ARBEID
                    ArenaHovedmal.OKEDELT -> OKE_DELTAKELSE
                    null -> null
                }
        }

    }
}

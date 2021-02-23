package no.nav.veilarbvedtaksstotte.domain.vedtak

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaHovedmal

data class Innsatsbehov(
    val aktorId: AktorId,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: HovedmalMedOkeDeltakelse?
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
                    ArenaHovedmal.SKAFFE_ARBEID -> HovedmalMedOkeDeltakelse.SKAFFE_ARBEID
                    ArenaHovedmal.BEHOLDE_ARBEID -> HovedmalMedOkeDeltakelse.BEHOLDE_ARBEID
                    ArenaHovedmal.OKE_DELTAKELSE -> HovedmalMedOkeDeltakelse.OKE_DELTAKELSE
                    null -> null
                }
        }

    }
}

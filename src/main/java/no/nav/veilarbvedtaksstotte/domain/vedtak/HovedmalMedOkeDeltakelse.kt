package no.nav.veilarbvedtaksstotte.domain.vedtak

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
        fun fraArenaHovedmal(arenaHovedmal: ArenaVedtak.ArenaHovedmal?): HovedmalMedOkeDeltakelse? =
            when (arenaHovedmal) {
                ArenaVedtak.ArenaHovedmal.SKAFFEA -> SKAFFE_ARBEID
                ArenaVedtak.ArenaHovedmal.BEHOLDEA -> BEHOLDE_ARBEID
                ArenaVedtak.ArenaHovedmal.OKEDELT -> OKE_DELTAKELSE
                null -> null
            }
    }

}
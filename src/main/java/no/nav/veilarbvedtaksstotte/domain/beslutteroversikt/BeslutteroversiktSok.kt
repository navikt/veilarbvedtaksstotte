package no.nav.veilarbvedtaksstotte.domain.beslutteroversikt

enum class BeslutteroversiktStatus {
    TRENGER_BESLUTTER,
    KLAR_TIL_BESLUTTER,
    KLAR_TIL_VEILEDER,
    GODKJENT_AV_BESLUTTER
}

data class BeslutteroversiktSokFilter(
    var enheter: List<String>? = null,
    var status: BeslutteroversiktStatus? = null,
    var visMineBrukere: Boolean = false
)

data class BeslutteroversiktSok(
    var fra: Int = 0,
    var antall: Int = 0,
    var filter: BeslutteroversiktSokFilter? = null,
    var orderByField: OrderByField? = null,
    var orderByDirection: OrderByDirection? = null
) {
    enum class OrderByField {
        BRUKER_ETTERNAVN,
        BRUKER_OPPFOLGINGSENHET_NAVN,
        BRUKER_FNR,
        VEDTAK_STARTET,
        STATUS,
        STATUS_ENDRET,
        BESLUTTER_NAVN,
        VEILEDER_NAVN
    }

    enum class OrderByDirection {
        ASC, DESC
    }
}

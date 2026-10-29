package no.nav.veilarbvedtaksstotte.domain.beslutteroversikt

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

data class BeslutteroversiktSok(
    val fra: Int = 0,
    val antall: Int = 0,
    @param:JsonSetter(nulls = Nulls.SKIP)
    val filter: BeslutteroversiktSokFilter = BeslutteroversiktSokFilter(),
    val orderByField: OrderByField? = null,
    val orderByDirection: OrderByDirection? = null,
) {
    // withX returnerer en kopi med endret felt, for Java-kode som mangler navngitte argumenter:
    // new BeslutteroversiktSok().withFilter(...).withFra(...).
    // Fjernes når ingen Java-kode bruker dem; Kotlin bruker konstruktøren eller copy.
    fun withFra(verdi: Int) = copy(fra = verdi)
    fun withAntall(verdi: Int) = copy(antall = verdi)
    fun withFilter(verdi: BeslutteroversiktSokFilter) = copy(filter = verdi)
    fun withOrderByField(verdi: OrderByField?) = copy(orderByField = verdi)
    fun withOrderByDirection(verdi: OrderByDirection?) = copy(orderByDirection = verdi)

    enum class OrderByField {
        BRUKER_ETTERNAVN,
        BRUKER_OPPFOLGINGSENHET_NAVN,
        BRUKER_FNR,
        VEDTAK_STARTET,
        STATUS,
        STATUS_ENDRET,
        BESLUTTER_NAVN,
        VEILEDER_NAVN,
    }

    enum class OrderByDirection {
        ASC,
        DESC,
    }
}

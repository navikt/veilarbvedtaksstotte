package no.nav.veilarbvedtaksstotte.domain.beslutteroversikt

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

data class BeslutteroversiktSokFilter(
    @param:JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.FAIL)
    val enheter: List<String> = emptyList(),
    val status: BeslutteroversiktStatus? = null,
    val visMineBrukere: Boolean = false,
) {
    // withX returnerer en kopi med endret felt, for Java-kode som mangler navngitte argumenter:
    // new BeslutteroversiktSokFilter().withEnheter(...).withStatus(...).
    // Fjernes når ingen Java-kode bruker dem; Kotlin bruker konstruktøren eller copy.
    fun withEnheter(verdi: List<String>) = copy(enheter = verdi)
    fun withStatus(verdi: BeslutteroversiktStatus?) = copy(status = verdi)
    fun withVisMineBrukere(verdi: Boolean) = copy(visMineBrukere = verdi)
}

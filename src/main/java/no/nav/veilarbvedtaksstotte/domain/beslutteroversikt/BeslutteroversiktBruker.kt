package no.nav.veilarbvedtaksstotte.domain.beslutteroversikt

import java.time.LocalDateTime

data class BeslutteroversiktBruker(
    val vedtakId: Long = 0,
    val brukerFornavn: String? = null,
    val brukerEtternavn: String? = null,
    val brukerOppfolgingsenhetNavn: String? = null,
    val brukerOppfolgingsenhetId: String? = null,
    val brukerFnr: String? = null,
    val vedtakStartet: LocalDateTime? = null,
    val status: BeslutteroversiktStatus? = null,
    val statusEndret: LocalDateTime? = null,
    val beslutterNavn: String? = null,
    val beslutterIdent: String? = null,
    val veilederNavn: String? = null,
) {
    // withX returnerer en kopi med endret felt, for Java-kode som mangler navngitte argumenter:
    // new BeslutteroversiktBruker().withVedtakId(...).withBrukerFnr(...).
    // Fjernes når ingen Java-kode bruker dem; Kotlin bruker konstruktøren eller copy.
    fun withVedtakId(verdi: Long) = copy(vedtakId = verdi)
    fun withBrukerFornavn(verdi: String?) = copy(brukerFornavn = verdi)
    fun withBrukerEtternavn(verdi: String?) = copy(brukerEtternavn = verdi)
    fun withBrukerOppfolgingsenhetNavn(verdi: String?) = copy(brukerOppfolgingsenhetNavn = verdi)
    fun withBrukerOppfolgingsenhetId(verdi: String?) = copy(brukerOppfolgingsenhetId = verdi)
    fun withBrukerFnr(verdi: String?) = copy(brukerFnr = verdi)
    fun withVedtakStartet(verdi: LocalDateTime?) = copy(vedtakStartet = verdi)
    fun withStatus(verdi: BeslutteroversiktStatus?) = copy(status = verdi)
    fun withStatusEndret(verdi: LocalDateTime?) = copy(statusEndret = verdi)
    fun withBeslutterNavn(verdi: String?) = copy(beslutterNavn = verdi)
    fun withBeslutterIdent(verdi: String?) = copy(beslutterIdent = verdi)
    fun withVeilederNavn(verdi: String?) = copy(veilederNavn = verdi)
}

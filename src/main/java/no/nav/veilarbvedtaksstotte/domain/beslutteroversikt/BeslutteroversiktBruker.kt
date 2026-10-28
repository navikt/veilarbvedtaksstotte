package no.nav.veilarbvedtaksstotte.domain.beslutteroversikt

import java.time.LocalDateTime

// Nullbarheten følger tabellen BESLUTTEROVERSIKT_BRUKER (V13__beslutteroversikt.sql).
// statusEndret er NOT NULL i tabellen, men nullbar her: databasen setter den ved innsetting,
// så den har ingen verdi før raden er lagret.
data class BeslutteroversiktBruker(
    val vedtakId: Long,
    val brukerFornavn: String,
    val brukerEtternavn: String,
    val brukerOppfolgingsenhetNavn: String,
    val brukerOppfolgingsenhetId: String,
    val brukerFnr: String,
    val vedtakStartet: LocalDateTime,
    val status: BeslutteroversiktStatus,
    val statusEndret: LocalDateTime?,
    val beslutterNavn: String?,
    val beslutterIdent: String?,
    val veilederNavn: String,
) {
    // withX returnerer en kopi med endret felt, for Java-kode som mangler navngitte argumenter og copy.
    // Fjernes når ingen Java-kode bruker dem.
    fun withVedtakId(verdi: Long) = copy(vedtakId = verdi)
    fun withBrukerFornavn(verdi: String) = copy(brukerFornavn = verdi)
    fun withBrukerEtternavn(verdi: String) = copy(brukerEtternavn = verdi)
    fun withBrukerOppfolgingsenhetNavn(verdi: String) = copy(brukerOppfolgingsenhetNavn = verdi)
    fun withBrukerOppfolgingsenhetId(verdi: String) = copy(brukerOppfolgingsenhetId = verdi)
    fun withBrukerFnr(verdi: String) = copy(brukerFnr = verdi)
    fun withVedtakStartet(verdi: LocalDateTime) = copy(vedtakStartet = verdi)
    fun withStatus(verdi: BeslutteroversiktStatus) = copy(status = verdi)
    fun withStatusEndret(verdi: LocalDateTime?) = copy(statusEndret = verdi)
    fun withBeslutterNavn(verdi: String?) = copy(beslutterNavn = verdi)
    fun withBeslutterIdent(verdi: String?) = copy(beslutterIdent = verdi)
    fun withVeilederNavn(verdi: String) = copy(veilederNavn = verdi)
}

package no.nav.veilarbvedtaksstotte.domain.vedtak

import java.util.*

data class KildeEntity(
    val tekst: String,
    val kildeId: UUID?
)

data class KildeForVedtak(
    val vedtakId: Long,
    val kilde: KildeEntity
)

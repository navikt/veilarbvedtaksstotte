package no.nav.veilarbvedtaksstotte.domain.statistikk

import java.math.BigInteger
import java.time.Instant

data class Siste14aSaksstatistikk(
    val id: BigInteger,
    val fattet_dato: Instant,
    val fraArena: Boolean,
)
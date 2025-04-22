package no.nav.veilarbvedtaksstotte.domain.statistikk

import java.math.BigInteger
import java.time.Instant

data class Siste14aSaksstatistikk(
    val id: BigInteger,
    val fattetDato: Instant,
    val fraArena: Boolean,
)
package no.nav.veilarbvedtaksstotte.domain.kafka

import java.time.ZonedDateTime
import java.util.*

data class KafkaSisteOppfolgingsperiode(
    val uuid: UUID?,
    val aktorId: String?,
    val startDato: ZonedDateTime?,
    val sluttDato: ZonedDateTime?
)

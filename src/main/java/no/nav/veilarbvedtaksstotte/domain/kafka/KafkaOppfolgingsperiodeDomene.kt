package no.nav.veilarbvedtaksstotte.domain.kafka

import java.time.ZonedDateTime
import java.util.*

data class KafkaSisteOppfolgingsperiode(
    val uuid: UUID?,
    val aktorId: String?,
    val startDato: ZonedDateTime?,
    val sluttDato: ZonedDateTime?
)

data class KafkaOppfolgingsperiode(
    val uuid: UUID?,
    val aktorId: String?,
    val startDato: ZonedDateTime?,
    val sluttDato: ZonedDateTime?,
    val startetBegrunnelse: StartetBegrunnelse
)

enum class StartetBegrunnelse {
    ARBEIDSSOKER,
    SYKEMELDT_MER_OPPFOLGING,
    MANUELL_REGISTRERING_VEILEDER
}

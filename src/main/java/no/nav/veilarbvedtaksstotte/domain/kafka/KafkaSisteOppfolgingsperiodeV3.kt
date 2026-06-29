package no.nav.veilarbvedtaksstotte.domain.kafka

import java.time.ZonedDateTime
import java.util.*

data class KontorDto(
    val kontorNavn: String,
    val kontorId: String,
)

enum class SisteEndringsType {
    OPPFOLGING_STARTET,
    ARBEIDSOPPFOLGINGSKONTOR_ENDRET,
    OPPFOLGING_AVSLUTTET
}

data class KafkaSisteOppfolgingsperiodeV3(
    val oppfolgingsperiodeUuid: UUID,
    val sisteEndringsType: SisteEndringsType,
    val aktorId: String,
    val ident: String,
    val startTidspunkt: ZonedDateTime,
    val sluttTidspunkt: ZonedDateTime?,
    val kontor: KontorDto?,
    val producerTimestamp: ZonedDateTime = ZonedDateTime.now(),
)
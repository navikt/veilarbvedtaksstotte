package no.nav.veilarbvedtaksstotte.domain.vedtak

import no.nav.common.types.identer.AktorId
import java.time.ZonedDateTime

data class Gjeldende14aVedtak(
    val aktorId: AktorId,
    val vedtakId: String?, //TODO hente verdi på ordentlig her
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: HovedmalMedOkeDeltakelse?,
    val fattetDato: ZonedDateTime
) {
    fun toGjeldende14aVedtakKafkaDTO(): Gjeldende14aVedtakKafkaDTO {
        return Gjeldende14aVedtakKafkaDTO(
            this.aktorId,
            this.innsatsgruppe,
            this.hovedmal
        )
    }
}

data class Gjeldende14aVedtakKafkaDTO(
    val aktorId: AktorId,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: HovedmalMedOkeDeltakelse?
)

fun Siste14aVedtak.toGjeldende14aVedtak(): Gjeldende14aVedtak = Gjeldende14aVedtak(
    aktorId = aktorId,
    vedtakId = null, //TODO hente verdi på ordentlig her
    innsatsgruppe = innsatsgruppe,
    hovedmal = hovedmal,
    fattetDato = fattetDato
)

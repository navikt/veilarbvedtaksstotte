package no.nav.veilarbvedtaksstotte.domain.vedtak

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.utils.TimeUtils
import java.time.ZonedDateTime

data class Gjeldende14aVedtakKafkaDTO(
    val aktorId: AktorId,
    val innsatsgruppe: InnsatsgruppeV2,
    val hovedmal: HovedmalMedOkeDeltakelse?,
    val fattetDato: ZonedDateTime,
    val vedtakId: String
)

fun Vedtak.toGjeldende14aVedtakKafkaDTO(): Gjeldende14aVedtakKafkaDTO {
    return Gjeldende14aVedtakKafkaDTO(
        aktorId = AktorId(aktorId),
        innsatsgruppe = innsatsgruppe.mapTilInnsatsgruppeV2(),
        hovedmal = HovedmalMedOkeDeltakelse.fraHovedmal(hovedmal),
        fattetDato = TimeUtils.toZonedDateTime(vedtakFattet),
        vedtakId = referanse.toString()
    )
}

fun Siste14aVedtak.toGjeldende14aVedtakKafkaDTO(): Gjeldende14aVedtakKafkaDTO {
    return Gjeldende14aVedtakKafkaDTO(
        aktorId = this.aktorId,
        innsatsgruppe = this.innsatsgruppe.mapTilInnsatsgruppeV2(),
        hovedmal = this.hovedmal,
        fattetDato = this.fattetDato,
        vedtakId = when (this.vedtakId) {
            is Siste14aVedtak.VedtakIdArena -> this.vedtakId.id.toString()
            is Siste14aVedtak.VedtakIdVedtaksstotte -> this.vedtakId.referanse.toString()
        }
    )
}

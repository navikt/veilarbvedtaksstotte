package no.nav.veilarbvedtaksstotte.domain.vedtak

import no.nav.common.types.identer.AktorId

data class Gjeldende14aVedtakKafkaDTO(
    val aktorId: AktorId,
    val innsatsgruppe: InnsatsgruppeV2,
    val hovedmal: HovedmalMedOkeDeltakelse?
)

fun Vedtak.toGjeldende14aVedtakKafkaDTO(): Gjeldende14aVedtakKafkaDTO {
    return Gjeldende14aVedtakKafkaDTO(
        aktorId = AktorId(aktorId),
        innsatsgruppe = innsatsgruppe.mapTilInnsatsgruppeV2(),
        hovedmal = HovedmalMedOkeDeltakelse.fraHovedmal(hovedmal)
    )
}

fun Siste14aVedtak.toGjeldende14aVedtakKafkaDTO(): Gjeldende14aVedtakKafkaDTO {
    return Gjeldende14aVedtakKafkaDTO(
        aktorId = aktorId,
        innsatsgruppe = innsatsgruppe.mapTilInnsatsgruppeV2(),
        hovedmal = hovedmal
    )
}
package no.nav.veilarbvedtaksstotte.domain.vedtak

import no.nav.common.types.identer.AktorId
import java.time.ZonedDateTime

data class Gjeldende14aVedtak(
    val aktorId: AktorId,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: HovedmalMedOkeDeltakelse?,
    val fattetDato: ZonedDateTime,
)

fun Siste14aVedtak.toGjeldende14aVedtak(): Gjeldende14aVedtak = Gjeldende14aVedtak(
    aktorId = aktorId,
    innsatsgruppe = innsatsgruppe,
    hovedmal = hovedmal,
    fattetDato = fattetDato
)

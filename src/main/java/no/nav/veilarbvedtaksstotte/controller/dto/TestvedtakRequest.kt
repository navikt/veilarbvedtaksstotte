package no.nav.veilarbvedtaksstotte.controller.dto

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.vedtak.*
import java.time.LocalDateTime

data class TestvedtakRequest(
    val fnr: Fnr,
    val innsatsgruppe: InnsatsgruppeV2,
    val hovedmal: Hovedmal? = null,
    val vedtakFattet: LocalDateTime? = null,
    val oppfolgingsEnhet: String,
)

fun TestvedtakRequest.toVedtak(aktorId: AktorId): Vedtak {
    val vedtak = Vedtak()
        .setAktorId(aktorId.get())
        .setHovedmal(this.hovedmal)
        .setInnsatsgruppe(this.innsatsgruppe.mapTilInnsatsgruppe())
        .setOppfolgingsenhetId(this.oppfolgingsEnhet)
        .setUtkastOpprettet(this.vedtakFattet ?: LocalDateTime.now())
        .setVedtakFattet(this.vedtakFattet ?: LocalDateTime.now())
        .setUtkastSistOppdatert(this.vedtakFattet ?: LocalDateTime.now())
    return vedtak
}
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

fun TestvedtakRequest.toVedtak(aktorId: AktorId): Vedtak = Vedtak()
        .settAktorId(aktorId.get())
        .settHovedmal(this.hovedmal)
        .settInnsatsgruppe(this.innsatsgruppe.mapTilInnsatsgruppe())
        .settOppfolgingsenhetId(this.oppfolgingsEnhet)
        .settUtkastOpprettet(this.vedtakFattet ?: LocalDateTime.now())
        .settVedtakFattet(this.vedtakFattet ?: LocalDateTime.now())
        .settUtkastSistOppdatert(this.vedtakFattet ?: LocalDateTime.now())

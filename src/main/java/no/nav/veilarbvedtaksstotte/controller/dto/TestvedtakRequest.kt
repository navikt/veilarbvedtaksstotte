package no.nav.veilarbvedtaksstotte.controller.dto

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.vedtak.*
import java.time.LocalDateTime
import java.util.*

data class OpprettTestvedtakRequest(
    val fnr: Fnr,
    val innsatsgruppe: InnsatsgruppeV2,
    val hovedmal: Hovedmal? = null,
    val vedtakFattet: LocalDateTime? = null,
    val oppfolgingsEnhet: String,
    val begrunnelse: String? = null,
    val veilederIdent: String? = "Ztestveileder"
)

data class TestvedtakRequest(
    val fnr: Fnr,
)
fun OpprettTestvedtakRequest.toVedtak(aktorId: AktorId): Vedtak = Vedtak()
        .settAktorId(aktorId.get())
        .settHovedmal(this.hovedmal)
        .settInnsatsgruppe(this.innsatsgruppe.mapTilInnsatsgruppe())
        .settOppfolgingsenhetId(this.oppfolgingsEnhet)
        .settUtkastOpprettet(this.vedtakFattet ?: LocalDateTime.now())
        .settVedtakFattet(this.vedtakFattet ?: LocalDateTime.now())
        .settUtkastSistOppdatert(this.vedtakFattet ?: LocalDateTime.now())
        .settReferanse(UUID.randomUUID())
        .settBegrunnelse(this.begrunnelse)
        .settVeilederIdent(this.veilederIdent)

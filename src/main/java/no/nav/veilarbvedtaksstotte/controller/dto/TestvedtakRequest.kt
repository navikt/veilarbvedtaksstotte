package no.nav.veilarbvedtaksstotte.controller.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.vedtak.*
import java.time.LocalDateTime
import java.util.*

data class OpprettTestvedtakRequest(
    @field:NotNull
    val fnr: Fnr,
    @field:NotNull
    val innsatsgruppe: InnsatsgruppeV2,
    val hovedmal: Hovedmal? = null,
    val vedtakFattet: LocalDateTime? = null,
    @field:NotBlank
    val oppfolgingsEnhet: String,
    val begrunnelse: String? = null,
    val veilederIdent: String? = "Ztestveileder"
)

data class TestvedtakRequest(
    @field:NotNull
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

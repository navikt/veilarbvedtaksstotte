package no.nav.veilarbvedtaksstotte.controller.dto

import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.KildeEntity
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus
import java.time.LocalDateTime

data class VedtakDTO(
    val id: Long,
    val hovedmal: Hovedmal?,
    val innsatsgruppe: Innsatsgruppe,
    val begrunnelse: String?,
    val veilederIdent: String,
    val veilederNavn: String,
    val oppfolgingsenhetId: String,
    val oppfolgingsenhetNavn: String,
    val beslutterIdent: String?,
    val beslutterNavn: String?,
    val opplysninger: List<String>,
    val kilder: List<KildeEntity>,
    val vedtakStatus: VedtakStatus,
    val utkastSistOppdatert: LocalDateTime,
    val gjeldende: Boolean,
    val vedtakFattet: LocalDateTime,
)

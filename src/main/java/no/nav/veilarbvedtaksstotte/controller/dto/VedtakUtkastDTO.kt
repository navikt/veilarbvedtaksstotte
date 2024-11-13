package no.nav.veilarbvedtaksstotte.controller.dto

import no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import java.time.LocalDateTime

data class VedtakUtkastDTO(
    val id: Long? = null,
    val hovedmal: Hovedmal? = null,
    val innsatsgruppe: Innsatsgruppe? = null,
    val utkastSistOppdatert: LocalDateTime? = null,
    val begrunnelse: String? = null,
    val veilederIdent: String? = null,
    val veilederNavn: String? = null,
    val oppfolgingsenhetId: String? = null,
    val oppfolgingsenhetNavn: String? = null,
    val beslutterIdent: String? = null,
    val beslutterNavn: String? = null,
    val opplysninger: List<String?>? = null,
    val beslutterProsessStatus: BeslutterProsessStatus? = null,
)
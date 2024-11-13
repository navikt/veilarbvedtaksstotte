package no.nav.veilarbvedtaksstotte.controller.dto

import net.minidev.json.annotate.JsonIgnore
import no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus
import java.time.LocalDateTime
import java.util.*

data class VedtakDTO(
    val id: Long? = null,
    val aktorId: String? = null,
    val hovedmal: Hovedmal? = null,
    val innsatsgruppe: Innsatsgruppe? = null,
    val vedtakStatus: VedtakStatus? = null,
    val utkastSistOppdatert: LocalDateTime? = null,
    val vedtakFattet: LocalDateTime? = null,
    val utkastOpprettet: LocalDateTime? = null,
    val begrunnelse: String? = null,
    val veilederIdent: String? = null,
    val veilederNavn: String? = null,
    val oppfolgingsenhetId: String? = null,
    val oppfolgingsenhetNavn: String? = null,
    val beslutterIdent: String? = null,
    val beslutterNavn: String? = null,
    val gjeldende: Boolean = false,
    val opplysninger: List<String?>? = null,
    val journalpostId: String? = null,
    val dokumentInfoId: String? = null,
    val dokumentbestillingId: String? = null,
    val beslutterProsessStatus: BeslutterProsessStatus? = null,
    val referanse: UUID? = null,
)
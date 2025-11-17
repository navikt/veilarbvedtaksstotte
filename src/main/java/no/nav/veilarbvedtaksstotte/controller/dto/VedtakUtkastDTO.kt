package no.nav.veilarbvedtaksstotte.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.KildeEntity
import java.time.LocalDateTime

data class VedtakUtkastDTO(
    @Schema(description = "ID-en til utkastet til § 14 a-vedtak")
    val id: Long? = null,
    @Schema(description = "Hovedmålet med oppfølgingen")
    val hovedmal: Hovedmal? = null,
    @Schema(description = "Innsatsgruppen som brukeren som vedtaket gjelder for kvalifiserer til")
    val innsatsgruppe: Innsatsgruppe? = null,
    @Schema(description = "Dato og tid for når vedtaket sist ble endret")
    val utkastSistOppdatert: LocalDateTime? = null,
    @Schema(description = "Begrunnelsen som ligger til grunn for oppfølgingen (innsatsgruppen) som brukeren vil ha krav på")
    val begrunnelse: String? = null,
    @Schema(description = "Identen til veilederen som er ansvarlig for vedtaket")
    val veilederIdent: String? = null,
    @Schema(description = "Navnet på veilederen som er ansvarlig for vedtaket")
    val veilederNavn: String? = null,
    @Schema(description = "ID-en til oppfølgingsenheten til brukeren som vedtaket gjelder for")
    val oppfolgingsenhetId: String? = null,
    @Schema(description = "Navnet på oppfølgingsenheten til brukeren som vedtaket gjelder for")
    val oppfolgingsenhetNavn: String? = null,
    @Schema(description = "Identen til beslutteren som er ansvarlig for vedtaket")
    val beslutterIdent: String? = null,
    @Schema(description = "Navnet på beslutteren som er ansvarlig for vedtaket")
    val beslutterNavn: String? = null,
    @Schema(description = "Opplysninger (kilder) som er vektlagt i vedtaket")
    val opplysninger: List<KildeEntity?>? = null,
    @Schema(description = "Nåværende steg i beslutterprosessen for vedtaket")
    val beslutterProsessStatus: BeslutterProsessStatus? = null,
    @Schema(description = "Indikerer om vedtaket kan distribueres til bruker")
    val kanDistribueres: Boolean? = null,
)
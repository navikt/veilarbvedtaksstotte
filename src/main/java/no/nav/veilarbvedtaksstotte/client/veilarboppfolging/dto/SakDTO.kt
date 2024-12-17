package no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class SakDTO(
    @JsonProperty("oppfolgingsperiodeId") val oppfolgingsperiodeId: UUID,
    @JsonProperty("sakId") val sakId: Long,
) {

    val fagsaksystem: String = "ARBEIDSOPPFOLGING"
    val tema: String = "OPP"
}
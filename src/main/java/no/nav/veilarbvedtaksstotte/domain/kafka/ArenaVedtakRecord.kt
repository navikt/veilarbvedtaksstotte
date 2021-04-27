package no.nav.veilarbvedtaksstotte.domain.kafka

import com.fasterxml.jackson.annotation.JsonAlias


data class ArenaVedtakRecord(
    val table: String,
    @JsonAlias("op_type") val opType: String,
    @JsonAlias("op_ts") val opTs: String,
    @JsonAlias("current_ts") val currentTs: String,
    val pos: String,
    val after: After
)

data class After(
    @JsonAlias("VEDTAK_ID") val vedtakId: Long,
    @JsonAlias("FRA_DATO") val fraDato: String?,
    @JsonAlias("REG_USER") val regUser: String?,
    @JsonAlias("FODSELSNR") val fnr: String,
    @JsonAlias("KVALIFISERINGSGRUPPEKODE") val kvalifiseringsgruppe: String,
    @JsonAlias("HOVEDMAALKODE") val hovedmal: String?,
    @JsonAlias("HENDELSE_ID") val hendelseId: Long
)

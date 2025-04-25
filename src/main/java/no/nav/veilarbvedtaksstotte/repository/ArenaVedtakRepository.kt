package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ArenaVedtakRepository(val jdbcTemplate: JdbcTemplate) {

    val namedParameterJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    companion object {
        const val ARENA_VEDTAK_TABLE = "ARENA_VEDTAK"
        const val FNR = "FNR"
        const val INNSATSGRUPPE = "INNSATSGRUPPE"
        const val HOVEDMAL = "HOVEDMAL"
        const val FRA_DATO = "FRA_DATO"
        const val REG_USER = "REG_USER"
        const val OPERATION_TIMESTAMP = "OPERATION_TIMESTAMP"
        const val HENDELSE_ID = "HENDELSE_ID"
        const val VEDTAK_ID = "VEDTAK_ID"
    }

    fun hentVedtakListe(fnrs: List<Fnr>): List<ArenaVedtak> {
        val parameters = MapSqlParameterSource("fnrs", fnrs.map { it.get() })
        val sql = "SELECT * FROM $ARENA_VEDTAK_TABLE WHERE $FNR IN(:fnrs)"

        return namedParameterJdbcTemplate.query(sql, parameters, arenaVedtakRowMapper)
    }

    fun hentUnikeBrukereMedVedtak(): List<Fnr> {
        val sql = "SELECT DISTINCT $FNR FROM $ARENA_VEDTAK_TABLE"

        return jdbcTemplate.query(sql) { rs, _ -> Fnr(rs.getString(FNR)) }
    }

    private val arenaVedtakRowMapper: RowMapper<ArenaVedtak> = RowMapper { rs, _ ->
        ArenaVedtak(
            fnr = Fnr(rs.getString(FNR)),
            innsatsgruppe = ArenaVedtak.ArenaInnsatsgruppe.valueOf(rs.getString(INNSATSGRUPPE)),
            hovedmal = rs.getString(HOVEDMAL)?.let { ArenaVedtak.ArenaHovedmal.valueOf(it) },
            fraDato = rs.getTimestamp(FRA_DATO).toLocalDateTime().toLocalDate(),
            regUser = rs.getString(REG_USER),
            operationTimestamp = rs.getTimestamp(OPERATION_TIMESTAMP).toLocalDateTime(),
            hendelseId = rs.getLong(HENDELSE_ID),
            vedtakId = rs.getLong(VEDTAK_ID)
        )
    }
}

package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.utils.DbUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class ArenaVedtakRepository(val jdbcTemplate: JdbcTemplate) {

    val namedParameterJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    val ARENA_VEDTAK_TABLE = "ARENA_VEDTAK"

    val FNR = "FNR"
    val INNSATSGRUPPE = "INNSATSGRUPPE"
    val HOVEDMAL = "HOVEDMAL"
    val FRA_DATO = "FRA_DATO" // TODO bedre navn?
    val REG_USER = "REG_USER"

    fun upsertVedtak(vedtak: ArenaVedtak) {
        val sql =
            """
                INSERT INTO $ARENA_VEDTAK_TABLE ($FNR, $INNSATSGRUPPE, $HOVEDMAL, $FRA_DATO, $REG_USER)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT ($FNR) DO UPDATE
                SET $INNSATSGRUPPE   = EXCLUDED.$INNSATSGRUPPE,
                    $HOVEDMAL        = EXCLUDED.$HOVEDMAL,
                    $FRA_DATO        = EXCLUDED.$FRA_DATO,
                    $REG_USER        = EXCLUDED.$REG_USER
            """

        jdbcTemplate.update(
            sql,
            vedtak.fnr.get(),
            vedtak.innsatsgruppe.name,
            vedtak.hovedmal?.name,
            vedtak.fraDato,
            vedtak.regUser
        )
    }

    fun hentVedtak(fnr: Fnr): ArenaVedtak? {
        val sql = "SELECT * FROM $ARENA_VEDTAK_TABLE WHERE $FNR = ?"
        return DbUtils.queryForObjectOrNull {
            jdbcTemplate.queryForObject(sql, this::arenaVedtakRowMapper, fnr.get())
        }
    }

    fun slettVedtak(fnrs: List<Fnr>): Int {
        val parameters = MapSqlParameterSource("fnrs", fnrs.map { it.get() })
        val sql = "DELETE FROM $ARENA_VEDTAK_TABLE WHERE $FNR IN(:fnrs)"
        return namedParameterJdbcTemplate.update(sql, parameters)
    }

    private fun arenaVedtakRowMapper(rs: ResultSet, row: Int): ArenaVedtak {
        return ArenaVedtak(
            fnr = Fnr(rs.getString(FNR)),
            innsatsgruppe = ArenaVedtak.ArenaInnsatsgruppe.valueOf(rs.getString(INNSATSGRUPPE)),
            hovedmal = rs.getString(HOVEDMAL)?.let { ArenaVedtak.ArenaHovedmal.valueOf(it) },
            fraDato = rs.getTimestamp(FRA_DATO).toLocalDateTime(),
            regUser = rs.getString(REG_USER)
        )
    }
}

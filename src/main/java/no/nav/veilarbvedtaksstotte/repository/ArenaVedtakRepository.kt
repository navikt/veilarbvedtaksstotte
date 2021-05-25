package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.utils.DbUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ArenaVedtakRepository(val jdbcTemplate: JdbcTemplate) {

    val namedParameterJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    val ARENA_VEDTAK_TABLE = "ARENA_VEDTAK"

    val FNR = "FNR"
    val INNSATSGRUPPE = "INNSATSGRUPPE"
    val HOVEDMAL = "HOVEDMAL"
    val FRA_DATO = "FRA_DATO"
    val REG_USER = "REG_USER"
    val OPERATION_TIMESTAMP = "OPERATION_TIMESTAMP"
    val HENDELSE_ID = "HENDELSE_ID"
    val VEDTAK_ID = "VEDTAK_ID"

    fun upsertVedtak(vedtak: ArenaVedtak) {
        val sql =
            """
                INSERT INTO $ARENA_VEDTAK_TABLE ($FNR, $INNSATSGRUPPE, $HOVEDMAL, $FRA_DATO, $REG_USER, $OPERATION_TIMESTAMP, $HENDELSE_ID, $VEDTAK_ID)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ($FNR) DO UPDATE
                SET $INNSATSGRUPPE        = EXCLUDED.$INNSATSGRUPPE,
                    $HOVEDMAL             = EXCLUDED.$HOVEDMAL,
                    $FRA_DATO             = EXCLUDED.$FRA_DATO,
                    $REG_USER             = EXCLUDED.$REG_USER,
                    $OPERATION_TIMESTAMP  = EXCLUDED.$OPERATION_TIMESTAMP,
                    $HENDELSE_ID          = EXCLUDED.$HENDELSE_ID,
                    $VEDTAK_ID            = EXCLUDED.$VEDTAK_ID
            """

        jdbcTemplate.update(
            sql,
            vedtak.fnr.get(),
            vedtak.innsatsgruppe.name,
            vedtak.hovedmal?.name,
            vedtak.fraDato,
            vedtak.regUser,
            vedtak.operationTimestamp,
            vedtak.hendelseId,
            vedtak.vedtakId
        )
    }

    fun hentVedtak(fnr: Fnr): ArenaVedtak? {
        val sql = "SELECT * FROM $ARENA_VEDTAK_TABLE WHERE $FNR = ?"
        return DbUtils.queryForObjectOrNull {
            jdbcTemplate.queryForObject(sql, arenaVedtakRowMapper, fnr.get())
        }
    }

    fun hentVedtakListe(fnrs: List<Fnr>): List<ArenaVedtak> {
        val parameters = MapSqlParameterSource("fnrs", fnrs.map { it.get() })
        val sql = "SELECT * FROM $ARENA_VEDTAK_TABLE WHERE $FNR IN(:fnrs)"

        return namedParameterJdbcTemplate.query(sql, parameters, arenaVedtakRowMapper)
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

package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.utils.DbUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class ArenaVedtakRepository(val jdbcTemplate: JdbcTemplate) {
    val ARENA_VEDTAK_TABLE = "ARENA_VEDTAK"

    val FNR = "FNR"
    val INNSATSGRUPPE = "INNSATSGRUPPE"
    val HOVEDMAL = "HOVEDMAL"
    val FRA_DATO = "FRA_DATO" // TODO bedre navn?
    val MOD_USER = "MOD_USER" // TODO må avklares

    fun upsertVedtak(vedtak: ArenaVedtak) {
        val sql =
            """
                INSERT INTO $ARENA_VEDTAK_TABLE ($FNR, $INNSATSGRUPPE, $HOVEDMAL, $FRA_DATO, $MOD_USER)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT ($FNR) DO UPDATE
                SET $INNSATSGRUPPE   = EXCLUDED.$INNSATSGRUPPE,
                    $HOVEDMAL        = EXCLUDED.$HOVEDMAL,
                    $FRA_DATO        = EXCLUDED.$FRA_DATO,
                    $MOD_USER        = EXCLUDED.$MOD_USER
            """

        jdbcTemplate.update(
            sql,
            vedtak.fnr.get(),
            vedtak.innsatsgruppe.name,
            vedtak.hovedmal?.name,
            vedtak.fraDato,
            vedtak.modUser
        )
    }

    fun hentVedtak(fnr: Fnr): ArenaVedtak? {
        val sql = "SELECT * FROM $ARENA_VEDTAK_TABLE WHERE $FNR = ?"
        return DbUtils.queryForObjectOrNull {
            jdbcTemplate.queryForObject(sql, this::arenaVedtakRowMapper, fnr.get())
        }
    }

    private fun arenaVedtakRowMapper(rs: ResultSet, row: Int): ArenaVedtak {
        return ArenaVedtak(
            fnr = Fnr(rs.getString(FNR)),
            innsatsgruppe = ArenaVedtak.ArenaInnsatsgruppe.valueOf(rs.getString(INNSATSGRUPPE)),
            hovedmal = rs.getString(HOVEDMAL)?.let { ArenaVedtak.ArenaHovedmal.valueOf(it) },
            fraDato = rs.getTimestamp(FRA_DATO).toLocalDateTime(),
            modUser = rs.getString(MOD_USER)
        )
    }
}

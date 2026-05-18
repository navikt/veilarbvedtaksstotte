package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.vedtak.*
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.*
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import no.nav.veilarbvedtaksstotte.utils.TimeUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.ZoneId

/**
 * Repository for håndtering av vedtak i testmiljø.
 * Her kan man lagre "fattede" vedtak direkte, hente de og slette de
 * Sletting skal aldri gjøres av proddata, kan kun  gjøres i testmiljø.
 */
@Repository
class TestvedtakRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    private val namedJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    fun lagreTestvedtak(vedtak: Vedtak, navConsumerId: String) {
        val begrunnelse = vedtak.begrunnelse ?: "Testvedtak opprettet for å teste vedtaksløsningen og videre flyt i preprod-miljøet."
        val sql = """
            INSERT INTO $VEDTAK_TABLE (
                $AKTOR_ID, $HOVEDMAL, $INNSATSGRUPPE, $OPPFOLGINGSENHET_ID,
                $UTKAST_SIST_OPPDATERT, $BEGRUNNELSE, $STATUS, $GJELDENDE,
                $UTKAST_OPPRETTET, $VEDTAK_FATTET, $VEILEDER_IDENT, $OPPRETTET_AV_SYSTEM
            ) VALUES (
                :aktorId, :hovedmal, :innsatsgruppe, :oppfolgingsenhetId,
                :utkastSistOppdatert, :begrunnelse, :status, true,
                :utkastOpprettet, :vedtakFattet, :veilederIdent, :opprettetAvSystem
            )
        """
        val params = MapSqlParameterSource()
            .addValue("aktorId", vedtak.aktorId)
            .addValue("hovedmal", vedtak.hovedmal.name)
            .addValue("innsatsgruppe", vedtak.innsatsgruppe.name)
            .addValue("oppfolgingsenhetId", vedtak.oppfolgingsenhetId)
            .addValue("utkastSistOppdatert", TimeUtils.toTimestampOrNull(vedtak.utkastSistOppdatert.atZone(ZoneId.systemDefault()).toInstant()))
            .addValue("begrunnelse", begrunnelse)
            .addValue("status", VedtakStatus.SENDT.name)
            .addValue("utkastOpprettet", TimeUtils.toTimestampOrNull(vedtak.utkastOpprettet.atZone(ZoneId.systemDefault()).toInstant()))
            .addValue("vedtakFattet", TimeUtils.toTimestampOrNull(vedtak.vedtakFattet.atZone(ZoneId.systemDefault()).toInstant()))
            .addValue("veilederIdent", vedtak.veilederIdent)
            .addValue("opprettetAvSystem", navConsumerId)

        namedJdbcTemplate.update(sql, params)
    }

    fun settTidligereVedtakIkkeGjeldende(aktorId: AktorId): Int {
        val sql = """UPDATE $VEDTAK_TABLE SET $GJELDENDE = false WHERE $AKTOR_ID = ? AND $GJELDENDE = true"""
        return jdbcTemplate.update(sql, aktorId.get())
    }

    fun hentGjeldendeTestvedtak(aktorId: AktorId): Vedtak? {
        val sql = """SELECT * FROM $VEDTAK_TABLE WHERE $AKTOR_ID = ? AND $GJELDENDE = true"""
        return jdbcTemplate.query(sql, this::vedtakMapper, aktorId.get()).firstOrNull()
    }

    fun slettGjeldendeTestvedtak(aktorId: AktorId, navConsumerId: String) {
        val sql = """DELETE FROM $VEDTAK_TABLE WHERE $AKTOR_ID = ? AND $GJELDENDE = true AND $OPPRETTET_AV_SYSTEM = ?"""
        val rowsDeleted = jdbcTemplate.update(sql, aktorId.get(), navConsumerId)
        if (rowsDeleted == 0) {
            secureLog.warn("Ingen testvedtak funnet å slette for aktorId: ${aktorId.get()} med opprettetAvSystem: $navConsumerId")
        }
    }

    private fun vedtakMapper(rs: ResultSet, row: Int): Vedtak {
        return Vedtak()
            .settId(rs.getLong(VEDTAK_ID))
            .settHovedmal(Hovedmal.valueOf(rs.getString(HOVEDMAL)))
            .settInnsatsgruppe(Innsatsgruppe.valueOf(rs.getString(INNSATSGRUPPE)))
            .settUtkastSistOppdatert(rs.getTimestamp(UTKAST_SIST_OPPDATERT).toLocalDateTime())
            .settVedtakFattet(rs.getTimestamp(VEDTAK_FATTET).toLocalDateTime())
            .settOppfolgingsenhetId(rs.getString(OPPFOLGINGSENHET_ID))
            .settVeilederIdent(rs.getString(VEILEDER_IDENT))
            .settAktorId(rs.getString(AKTOR_ID))
            .settGjeldende(rs.getBoolean(GJELDENDE))
    }
}

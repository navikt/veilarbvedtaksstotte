package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakType
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.AKTOR_ID
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.BEGRUNNELSE
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.GJELDENDE
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.HOVEDMAL
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.INNSATSGRUPPE
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.OPPFOLGINGSENHET_ID
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.STATUS
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.UTKAST_OPPRETTET
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.UTKAST_SIST_OPPDATERT
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.VEDTAK_FATTET
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.VEDTAK_ID
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.VEDTAK_TABLE
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.VEDTAK_TYPE
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.VEILEDER_IDENT
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

    fun lagreTestvedtak(vedtak: Vedtak) {
        val begrunnelse = vedtak.begrunnelse ?: "Testvedtak opprettet for å teste vedtaksløsningen og videre flyt i preprod-miljøet."
        val sql = """
            INSERT INTO $VEDTAK_TABLE (
                $AKTOR_ID, $HOVEDMAL, $INNSATSGRUPPE, $OPPFOLGINGSENHET_ID,
                $UTKAST_SIST_OPPDATERT, $BEGRUNNELSE, $STATUS, $GJELDENDE,
                $UTKAST_OPPRETTET, $VEDTAK_FATTET, $VEILEDER_IDENT, $VEDTAK_TYPE
            ) VALUES (
                :aktorId, :hovedmal, :innsatsgruppe, :oppfolgingsenhetId,
                :utkastSistOppdatert, :begrunnelse, :status, true,
                :utkastOpprettet, :vedtakFattet, :veilederIdent, :vedtakType
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
            .addValue("vedtakType", VedtakType.TEST_VEDTAK.name)

        namedJdbcTemplate.update(sql, params)
    }

    fun settTidligereTestvedtakIkkeGjeldende(aktorId: AktorId): Int {
        val sql = """UPDATE $VEDTAK_TABLE SET $GJELDENDE = false WHERE $AKTOR_ID = ? AND $GJELDENDE = true AND $VEDTAK_TYPE = ?"""
        return jdbcTemplate.update(sql, aktorId.get(), VedtakType.TEST_VEDTAK.name)
    }

    fun hentGjeldendeTestvedtak(aktorId: AktorId): Vedtak? {
        val sql = """SELECT * FROM $VEDTAK_TABLE WHERE $AKTOR_ID = ? AND $GJELDENDE = true AND $VEDTAK_TYPE = ?"""
        return jdbcTemplate.query(sql, this::vedtakMapper, aktorId.get(), VedtakType.TEST_VEDTAK.name).firstOrNull()
    }

    fun hentAlleTestvedtak(aktorId: AktorId): List<Vedtak> {
        val sql = """SELECT * FROM $VEDTAK_TABLE WHERE $AKTOR_ID = ? AND $VEDTAK_TYPE = ?"""
        return jdbcTemplate.query(sql, this::vedtakMapper, aktorId.get(), VedtakType.TEST_VEDTAK.name)
    }

    fun slettGjeldendeTestvedtak(aktorId: AktorId) {
        val sql = """DELETE FROM $VEDTAK_TABLE WHERE $AKTOR_ID = ? AND $GJELDENDE = true AND $VEDTAK_TYPE = ?"""
        val rowsDeleted = jdbcTemplate.update(sql, aktorId.get(), VedtakType.TEST_VEDTAK.name)
        if (rowsDeleted == 0) {
            secureLog.warn("Ingen testvedtak funnet å slette for aktorId: ${aktorId.get()} med vedtakType: ${VedtakType.TEST_VEDTAK.name}")
        }
    }

    private fun vedtakMapper(rs: ResultSet, _row: Int): Vedtak {
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

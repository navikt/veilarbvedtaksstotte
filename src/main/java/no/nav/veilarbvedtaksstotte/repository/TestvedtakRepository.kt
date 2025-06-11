package no.nav.veilarbvedtaksstotte.repository

import lombok.SneakyThrows
import no.nav.common.types.identer.AktorId
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarbvedtaksstotte.domain.vedtak.*
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.*
import no.nav.veilarbvedtaksstotte.utils.EnumUtils
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import no.nav.veilarbvedtaksstotte.utils.TimeUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
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

    @Transactional
    fun lagreTestvedtak(vedtak: Vedtak, navConsumerId: String) {
        if (!EnvironmentUtils.isDevelopment().orElse(false)) {
            throw UnsupportedOperationException("Lagring av vedtak er kun støttet i preprod-miljøer for testing av ny vedtaksløsning.")
        }
        val begrunnelse = vedtak.begrunnelse ?: "Testvedtak opprettet for å teste vedtaksløsningen og videre flyt i preprod-miljøet."
        val sql =
            """
                INSERT INTO $VEDTAK_TABLE ($AKTOR_ID, $HOVEDMAL, $INNSATSGRUPPE, $OPPFOLGINGSENHET_ID, $UTKAST_SIST_OPPDATERT, $BEGRUNNELSE, $STATUS, $GJELDENDE, $UTKAST_OPPRETTET, $VEDTAK_FATTET, $VEILEDER_IDENT, $OPPRETTET_AV_SYSTEM)
                VALUES (?, ?, ?, ?, ?, ?, ?, true, ?, ?, ?, ?)
            """

        jdbcTemplate.update(
            sql,
            vedtak.aktorId,
            vedtak.hovedmal.name,
            vedtak.innsatsgruppe.name,
            vedtak.oppfolgingsenhetId,
            TimeUtils.toTimestampOrNull(vedtak.utkastSistOppdatert.atZone(ZoneId.of("Europe/Oslo")).toInstant()),
            begrunnelse,
            VedtakStatus.SENDT.name,
            TimeUtils.toTimestampOrNull(vedtak.utkastOpprettet.atZone(ZoneId.of("Europe/Oslo")).toInstant()),
            TimeUtils.toTimestampOrNull(vedtak.vedtakFattet.atZone(ZoneId.of("Europe/Oslo")).toInstant()),
            vedtak.veilederIdent,
            navConsumerId
        )
    }

    fun settTidligereVedtakIkkeGjeldende(aktorId: AktorId): Int {
        if (!EnvironmentUtils.isDevelopment().orElse(false)) {
            throw UnsupportedOperationException("Oppdatering av tidligere vedtak er kun støttet i preprod-miljøer for testing av ny vedtaksløsning.")
        }

        val sql = """UPDATE $VEDTAK_TABLE SET $GJELDENDE = false WHERE $AKTOR_ID = ? AND $GJELDENDE = true"""
        return jdbcTemplate.update(sql, aktorId.get())

    }

    fun hentTestvedtak(aktorId: AktorId): Vedtak? {
        try {
            val sql = """SELECT * FROM $VEDTAK_TABLE WHERE $AKTOR_ID = ?"""
            return jdbcTemplate.query(sql, this::vedtakMapper, aktorId.get()).firstOrNull()
        } catch (e: Exception) {
            return null
        }
    }

    fun slettTestvedtak(aktorId: AktorId) {
        if (!EnvironmentUtils.isDevelopment().orElse(false)) {
            throw UnsupportedOperationException("Sletting av vedtak er kun støttet i preprod-miljøer.")
        }
        try {
            val sql = """DELETE FROM $VEDTAK_TABLE WHERE $AKTOR_ID = ?"""
            jdbcTemplate.update(sql, aktorId.get())
        } catch (e: Exception) {
            secureLog.error("Kunne ikke slette testvedtak for aktorId: ${aktorId.get()}. Feil: ${e.message}", e)
            throw RuntimeException("Kunne ikke slette testvedtak")
        }
    }

    @SneakyThrows
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
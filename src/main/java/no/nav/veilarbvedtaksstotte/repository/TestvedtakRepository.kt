package no.nav.veilarbvedtaksstotte.repository

import lombok.SneakyThrows
import no.nav.common.types.identer.AktorId
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarbvedtaksstotte.domain.vedtak.*
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.*
import no.nav.veilarbvedtaksstotte.utils.EnumUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

/**
 * Repository for håndtering av vedtak i testmiljø.
 * Her kan man lagre "fattede" vedtak direkte, hente de og slette de
 * Sletting skal aldri gjøres av proddata, kan kun  gjøres i testmiljø.
 */
@Repository
class TestvedtakRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    fun lagreTestvedtak(vedtak: Vedtak) {
        if (!EnvironmentUtils.isDevelopment().orElse(false)) {
            throw UnsupportedOperationException("Lagring av vedtak er kun støttet i preprod-miljøer for testing av ny vedtaksløsning.")
        }
        val sql =
            """
                INSERT INTO $VEDTAK_TABLE ($AKTOR_ID, $HOVEDMAL, $INNSATSGRUPPE, $OPPFOLGINGSENHET_ID, $UTKAST_SIST_OPPDATERT, $BEGRUNNELSE, $STATUS, $GJELDENDE, $UTKAST_OPPRETTET, $VEDTAK_FATTET, $VEILEDER_IDENT)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ($AKTOR_ID) DO UPDATE
                SET $HOVEDMAL             = EXCLUDED.$HOVEDMAL,
                    $INNSATSGRUPPE        = EXCLUDED.$INNSATSGRUPPE,
                    $OPPFOLGINGSENHET_ID = EXCLUDED.$OPPFOLGINGSENHET_ID,
                    $UTKAST_SIST_OPPDATERT = EXCLUDED.$UTKAST_SIST_OPPDATERT,
                    $BEGRUNNELSE = EXCLUDED.$BEGRUNNELSE,
                    $STATUS = EXCLUDED.$STATUS,
                    $GJELDENDE = EXCLUDED.$GJELDENDE,
                    $UTKAST_OPPRETTET = EXCLUDED.$UTKAST_OPPRETTET,
                    $VEDTAK_FATTET = EXCLUDED.$VEDTAK_FATTET,
                    $VEILEDER_IDENT = EXCLUDED.$VEILEDER_IDENT
            """

        jdbcTemplate.update(
            sql,
            vedtak.aktorId,
            vedtak.hovedmal.name,
            vedtak.innsatsgruppe.name,
            vedtak.oppfolgingsenhetId,
            vedtak.utkastSistOppdatert,
            vedtak.begrunnelse,
            vedtak.vedtakStatus,
            vedtak.isGjeldende,
            vedtak.utkastOpprettet,
            vedtak.vedtakFattet,
            vedtak.veilederIdent
        )
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
        val sql = """DELETE FROM $VEDTAK_TABLE WHERE $AKTOR_ID = ?"""
        jdbcTemplate.update(sql, aktorId.get())
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
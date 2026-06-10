package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.oppfolgingsperiode.SisteOppfolgingsperiode
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import no.nav.veilarbvedtaksstotte.utils.TimeUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime
import java.util.*

@Repository
class SisteOppfolgingPeriodeRepository(val jdbcTemplate: JdbcTemplate) {

    val SISTE_OPPFOLGING_PERIODE_TABELL = "SISTE_OPPFOLGING_PERIODE"
    val OPPFOLGINGSPERIODE_ID = "OPPFOLGINGSPERIODE_ID"
    val AKTORID = "AKTORID"
    val STARTDATO = "STARTDATO"
    val SLUTTDATO = "SLUTTDATO"

    fun hentInnevaerendeOppfolgingsperiode(aktorId: AktorId): SisteOppfolgingsperiode? {
        val sql =
            """
                SELECT * FROM $SISTE_OPPFOLGING_PERIODE_TABELL 
                WHERE $AKTORID = ?
                AND $SLUTTDATO IS NULL;
            """.trimIndent()

        try {
            return jdbcTemplate.queryForObject(sql, sisteOppfolgingsperiodeRowMapper, aktorId.get())
        } catch (e: Exception) {
            secureLog.error(
                "Kunne ikke hente siste oppfolgingsperiode for aktorId: $aktorId",
            )
            return null
        }
    }

    fun upsertSisteOppfolgingPeriode(
        oppfolgingsperiodeUuid: UUID, aktorId: String, startTidspunkt: ZonedDateTime,
        sluttTidspunkt: ZonedDateTime?
    ) {
        val sql =
            """
                INSERT INTO $SISTE_OPPFOLGING_PERIODE_TABELL ($OPPFOLGINGSPERIODE_ID, $AKTORID, $STARTDATO, $SLUTTDATO)
                VALUES (?, ?, ?, ?)
                ON CONFLICT ($AKTORID) DO UPDATE
                SET $OPPFOLGINGSPERIODE_ID = EXCLUDED.$OPPFOLGINGSPERIODE_ID,
                    $STARTDATO = EXCLUDED.$STARTDATO,
                    $SLUTTDATO = EXCLUDED.$SLUTTDATO
            """
        try {
            jdbcTemplate.update(
                sql,
                oppfolgingsperiodeUuid,
                aktorId,
                TimeUtils.toTimestampOrNull(startTidspunkt.toInstant()),
                TimeUtils.toTimestampOrNull(sluttTidspunkt?.toInstant())
            )
        } catch (e: Exception) {
            secureLog.error("Kunne ikke lagre sisteOppfolgingPeriode, feil: {} , oppfolgingsperiodeUuid {}, aktorId {}, startTidspunkt {}, sluttTidspunkt {}", e, oppfolgingsperiodeUuid, aktorId, startTidspunkt, sluttTidspunkt.toString())
        }
    }

    private val sisteOppfolgingsperiodeRowMapper: RowMapper<SisteOppfolgingsperiode> = RowMapper { rs, _ ->
        SisteOppfolgingsperiode(
            oppfolgingsperiodeId = UUID.fromString(rs.getString(OPPFOLGINGSPERIODE_ID)),
            aktorId = AktorId.of(rs.getString(AKTORID)),
            startdato = TimeUtils.toZonedDateTime(rs.getTimestamp(STARTDATO))!!,
            sluttdato = TimeUtils.toZonedDateTime(rs.getTimestamp(SLUTTDATO))
        )
    }
}

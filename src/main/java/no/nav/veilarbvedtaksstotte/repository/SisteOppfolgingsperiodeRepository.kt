package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaSisteOppfolgingsperiode
import no.nav.veilarbvedtaksstotte.domain.oppfolgingsperiode.SisteOppfolgingsperiode
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import no.nav.veilarbvedtaksstotte.utils.TimeUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class SisteOppfolgingPeriodeRepository(val jdbcTemplate: JdbcTemplate) {

    val SISTE_OPPFOLGING_PERIODE_TABELL = "SISTE_OPPFOLGING_PERIODE"
    val OPPFOLGINGSPERIODE_ID = "OPPFOLGINGSPERIODE_ID"
    val AKTORID = "AKTORID"
    val STARTDATO = "STARTDATO"
    val SLUTTDATO = "SLUTTDATO"

    fun hentInnevarendeOppfolgingsperiode(aktorId: AktorId): SisteOppfolgingsperiode? {
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

    fun upsertSisteOppfolgingPeriode(sisteOppfolgingPeriode: KafkaSisteOppfolgingsperiode) {
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
                sisteOppfolgingPeriode.uuid,
                sisteOppfolgingPeriode.aktorId,
                TimeUtils.toTimestampOrNull(sisteOppfolgingPeriode.startDato?.toInstant()),
                TimeUtils.toTimestampOrNull(sisteOppfolgingPeriode.sluttDato?.toInstant())
            )
        } catch (e: Exception) {
            secureLog.error("Kunne ikke lagre sisteOppfolgingPeriode, feil: {} , sisteOppfolgingPeriode: {}", e, sisteOppfolgingPeriode)
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

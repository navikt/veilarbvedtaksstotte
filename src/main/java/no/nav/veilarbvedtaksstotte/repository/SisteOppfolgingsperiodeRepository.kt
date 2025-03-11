package no.nav.veilarbvedtaksstotte.repository

import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaSisteOppfolgingsperiode
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import no.nav.veilarbvedtaksstotte.utils.TimeUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class SisteOppfolgingPeriodeRepository(val jdbcTemplate: JdbcTemplate) {

    val SISTE_OPPFOLGING_PERIODE_TABELL = "SISTE_OPPFOLGING_PERIODE"
    val OPPFOLGINGSPERIODE_ID = "OPPFOLGINGSPERIODE_ID"
    val AKTORID = "AKTORID"
    val STARTDATO = "STARTDATO"
    val SLUTTDATO = "SLUTTDATO"

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
}
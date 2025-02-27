package no.nav.veilarbvedtaksstotte.repository

import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsperiode
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import no.nav.veilarbvedtaksstotte.utils.TimeUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class OppfolgingsperiodeRepository(val jdbcTemplate: JdbcTemplate) {

    val OPPFOLGINGSPERIODE_TABLE = "OPPFOLGINGSPERIODE"
    val OPPFOLGINGSPERIODEUUID = "OPPFOLGINGSPERIODE_ID"
    val AKTORID = "AKTORID"
    val STARTDATO = "STARTDATO"
    val SLUTTDATO = "SLUTTDATO"
    val STARTET_BEGRUNNELSE = "STARTET_BEGRUNNELSE"

    fun insertOppfolgingsperiode(oppfolgingsperiode: KafkaOppfolgingsperiode) {
        val sql =
            """
                INSERT INTO $OPPFOLGINGSPERIODE_TABLE ($OPPFOLGINGSPERIODEUUID, $AKTORID, $STARTDATO, $SLUTTDATO, $STARTET_BEGRUNNELSE)
                VALUES (?, ?, ?, ?, ?)
            """
        try {
            jdbcTemplate.update(
                sql,
                oppfolgingsperiode.uuid,
                oppfolgingsperiode.aktorId,
                TimeUtils.toTimestampOrNull(oppfolgingsperiode.startDato?.toInstant()),
                TimeUtils.toTimestampOrNull(oppfolgingsperiode.sluttDato?.toInstant()),
                oppfolgingsperiode.startetBegrunnelse.name
            )
        } catch (e: Exception) {
            secureLog.error(
                "Kunne ikke lagre oppfølgingsperiode, feil: {} , oppfolgingsperiode: {}",
                e,
                oppfolgingsperiode
            )
        }
    }
}
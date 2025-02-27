package no.nav.veilarbvedtaksstotte.repository

import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsperiode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class OppfolgingsperiodeRepository(val jdbcTemplate: JdbcTemplate) {

    private val log: Logger = LoggerFactory.getLogger(SakStatistikkRepository::class.java)

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
           jdbcTemplate.update(sql, oppfolgingsperiode.uuid, oppfolgingsperiode.aktorId, oppfolgingsperiode.startDato, oppfolgingsperiode.sluttDato, oppfolgingsperiode.startetBegrunnelse)
        } catch (e: Exception) {
            log.error("Kunne ikke lagre oppfølgingsperiode, feil: {} , oppfolgingsperiode: {}", e, oppfolgingsperiode)
        }
    }
}
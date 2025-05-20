package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingMetode
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingResultat
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingStatus
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingType
import no.nav.veilarbvedtaksstotte.domain.statistikk.Fagsystem
import no.nav.veilarbvedtaksstotte.domain.statistikk.HovedmalNy
import no.nav.veilarbvedtaksstotte.domain.statistikk.SakStatistikk
import no.nav.veilarbvedtaksstotte.domain.statistikk.Siste14aSaksstatistikk
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.utils.TimeUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigInteger
import java.time.ZonedDateTime
import java.util.*

@Repository
class SakStatistikkRepository(val jdbcTemplate: JdbcTemplate) {

    private val log: Logger = LoggerFactory.getLogger(SakStatistikkRepository::class.java)

    val namedParameterJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    companion object {
        const val SAK_STATISTIKK_TABLE = "SAK_STATISTIKK"
        const val SEKVENSNUMMER = "SEKVENSNUMMER"
        const val AKTOR_ID = "AKTOR_ID"
        const val OPPFOLGING_PERIODE_UUID = "OPPFOLGING_PERIODE_UUID"
        const val BEHANDLING_ID = "BEHANDLING_ID"
        const val RELATERT_BEHANDLING_ID = "RELATERT_BEHANDLING_ID"
        const val RELATERT_FAGSYSTEM = "RELATERT_FAGSYSTEM"
        const val SAK_ID = "SAK_ID"
        const val MOTTATT_TID = "MOTTATT_TID"
        const val REGISTRERT_TID = "REGISTRERT_TID"
        const val FERDIGBEHANDLET_TID = "FERDIGBEHANDLET_TID"
        const val ENDRET_TID = "ENDRET_TID"
        const val SAK_YTELSE = "SAK_YTELSE"
        const val BEHANDLING_TYPE = "BEHANDLING_TYPE"
        const val BEHANDLING_STATUS = "BEHANDLING_STATUS"
        const val BEHANDLING_RESULTAT = "BEHANDLING_RESULTAT"
        const val BEHANDLING_METODE = "BEHANDLING_METODE"
        const val INNSATSGRUPPE = "INNSATSGRUPPE"
        const val HOVEDMAL = "HOVEDMAL"
        const val OPPRETTET_AV = "OPPRETTET_AV"
        const val SAKSBEHANDLER = "SAKSBEHANDLER"
        const val ANSVARLIG_BESLUTTER = "ANSVARLIG_BESLUTTER"
        const val ANSVARLIG_ENHET = "ANSVARLIG_ENHET"
        const val FAGSYSTEM_NAVN = "FAGSYSTEM_NAVN"
        const val FAGSYSTEM_VERSJON = "FAGSYSTEM_VERSJON"
    }

    fun insertSakStatistikkRad(sakStatistikkRad: SakStatistikk): Long? {
        val sql =
            """
                INSERT INTO $SAK_STATISTIKK_TABLE ($AKTOR_ID, $OPPFOLGING_PERIODE_UUID, $BEHANDLING_ID, $RELATERT_BEHANDLING_ID,
                $RELATERT_FAGSYSTEM, $SAK_ID, $MOTTATT_TID, $REGISTRERT_TID, $FERDIGBEHANDLET_TID,
                $ENDRET_TID, $SAK_YTELSE, $BEHANDLING_TYPE, $BEHANDLING_STATUS, 
                $BEHANDLING_RESULTAT, $BEHANDLING_METODE, $INNSATSGRUPPE, $HOVEDMAL, $OPPRETTET_AV, $SAKSBEHANDLER, $ANSVARLIG_BESLUTTER,
                $ANSVARLIG_ENHET, $FAGSYSTEM_NAVN, $FAGSYSTEM_VERSJON)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING $SEKVENSNUMMER
            """
        return try {
            jdbcTemplate.queryForObject(
                sql,
                Long::class.java,
                sakStatistikkRad.aktorId?.get(),
                sakStatistikkRad.oppfolgingPeriodeUUID,
                sakStatistikkRad.behandlingId,
                sakStatistikkRad.relatertBehandlingId,
                sakStatistikkRad.relatertFagsystem?.name,
                sakStatistikkRad.sakId,
                TimeUtils.toTimestampOrNull(sakStatistikkRad.mottattTid),
                TimeUtils.toTimestampOrNull(sakStatistikkRad.registrertTid),
                TimeUtils.toTimestampOrNull(sakStatistikkRad.ferdigbehandletTid),
                TimeUtils.toTimestampOrNull(sakStatistikkRad.endretTid),
                sakStatistikkRad.sakYtelse,
                sakStatistikkRad.behandlingType?.name,
                sakStatistikkRad.behandlingStatus?.name,
                sakStatistikkRad.behandlingResultat?.name,
                sakStatistikkRad.behandlingMetode?.name,
                sakStatistikkRad.innsatsgruppe?.name,
                sakStatistikkRad.hovedmal?.name,
                sakStatistikkRad.opprettetAv,
                sakStatistikkRad.saksbehandler,
                sakStatistikkRad.ansvarligBeslutter,
                sakStatistikkRad.ansvarligEnhet?.get(),
                sakStatistikkRad.fagsystemNavn.name,
                sakStatistikkRad.fagsystemVersjon
            )
        } catch (e: Exception) {
            log.error("Kunne ikke lagre sakStatistikkRad, feil: {} , sakStatistikkRad: {}", e, sakStatistikkRad)
            null
        }
    }

    fun insertSakStatistikkRadBatch(sakStatistikkRader: List<SakStatistikk>): List<SakStatistikk> {
        if (sakStatistikkRader.isEmpty()) return emptyList()

        val params = sakStatistikkRader.flatMap { statistikkrad ->
            listOf(
                statistikkrad.behandlingId,
                statistikkrad.aktorId?.get(),
                statistikkrad.relatertBehandlingId,
                statistikkrad.relatertFagsystem?.name,
                statistikkrad.sakId,
                TimeUtils.toTimestampOrNull(statistikkrad.mottattTid),
                TimeUtils.toTimestampOrNull(statistikkrad.registrertTid),
                TimeUtils.toTimestampOrNull(statistikkrad.ferdigbehandletTid),
                TimeUtils.toTimestampOrNull(statistikkrad.endretTid),
                statistikkrad.sakYtelse,
                statistikkrad.behandlingType?.name,
                statistikkrad.behandlingStatus?.name,
                statistikkrad.behandlingResultat?.name,
                statistikkrad.behandlingMetode?.name,
                statistikkrad.opprettetAv,
                statistikkrad.saksbehandler,
                statistikkrad.ansvarligBeslutter,
                statistikkrad.ansvarligEnhet?.get(),
                statistikkrad.fagsystemNavn.name,
                statistikkrad.fagsystemVersjon,
                statistikkrad.oppfolgingPeriodeUUID,
                statistikkrad.innsatsgruppe?.name,
                statistikkrad.hovedmal?.name,
            )
        }

        // Pass på at rekkefølgen på kolonnene i SQL-spørringen er den samme som rekkefølgen på parameterne
        val sql = """
                    INSERT INTO $SAK_STATISTIKK_TABLE ($BEHANDLING_ID, $AKTOR_ID, $RELATERT_BEHANDLING_ID, $RELATERT_FAGSYSTEM, 
                    $SAK_ID, $MOTTATT_TID, $REGISTRERT_TID, $FERDIGBEHANDLET_TID,
                    $ENDRET_TID, $SAK_YTELSE, $BEHANDLING_TYPE, $BEHANDLING_STATUS,
                    $BEHANDLING_RESULTAT, $BEHANDLING_METODE, $OPPRETTET_AV, $SAKSBEHANDLER, $ANSVARLIG_BESLUTTER,
                    $ANSVARLIG_ENHET, $FAGSYSTEM_NAVN, $FAGSYSTEM_VERSJON, $OPPFOLGING_PERIODE_UUID, $INNSATSGRUPPE, $HOVEDMAL)
                    VALUES 
                """ +
                // Genererer en liste med ? for hver rad i sakStatistikkRader
                sakStatistikkRader.joinToString(", ") { "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" } +
                "RETURNING *"

        return jdbcTemplate.query(sql, sakStatistikkRowMapper, *params.toTypedArray())
    }

    fun hentForrigeVedtakFraSammeOppfolgingsperiode(startOppfolgingsperiodeDato: ZonedDateTime, aktorId: AktorId, fnr: Fnr, gjeldendeVedtakId: BigInteger): Siste14aSaksstatistikk? {
        val sql = """
            SELECT vedtak_id as id, fra_dato as fattet_dato, 'ARENA' AS kilde
            FROM ARENA_VEDTAK
            WHERE FNR = ?
            AND FRA_DATO > ?
            UNION ALL
            SELECT id, vedtak_fattet as fattet_dato, 'VEDTAKSSTOTTE' AS kilde
            FROM VEDTAK
            WHERE AKTOR_ID = ?
            AND VEDTAK_FATTET > ? AND ID != ?
        """.trimIndent()
        return jdbcTemplate.query(
            sql,
            { rs, _ ->
                Siste14aSaksstatistikk(
                    id = rs.getString("id").toBigInteger(),
                    fattetDato = rs.getTimestamp("fattet_dato").toInstant(),
                    fraArena = rs.getString("kilde") == "ARENA"
                )
            },
            fnr.get(),
            TimeUtils.toTimestampOrNull(startOppfolgingsperiodeDato.toInstant()),
            aktorId.get(),
            TimeUtils.toTimestampOrNull(startOppfolgingsperiodeDato.toInstant()),
            gjeldendeVedtakId
        ).maxByOrNull { it.fattetDato }
    }

    fun hentOpprettetAvFraVedtak(vedtak: Vedtak): String? {
        val sql = "SELECT $OPPRETTET_AV FROM $SAK_STATISTIKK_TABLE WHERE $BEHANDLING_ID = ? ORDER BY $SEKVENSNUMMER LIMIT 1"
        return try {
            jdbcTemplate.queryForObject(sql, String::class.java, vedtak.id)
        } catch (e: Exception) {
            log.info("Kunne ikke hente opprettetAv for vedtakId: ${vedtak.id}, bruker veileder som gjorde siste handling", e)
            vedtak.veilederIdent
        }
    }

    fun hentSisteHendelsePaaVedtak(behandlingId: BigInteger): SakStatistikk? {
        val sql = "SELECT * FROM $SAK_STATISTIKK_TABLE WHERE $BEHANDLING_ID = ? ORDER BY $SEKVENSNUMMER DESC LIMIT 1"
        return try {
            jdbcTemplate.query(sql, sakStatistikkRowMapper, behandlingId).first()
        } catch (e: Exception) {
            log.info("Fantes ingen tidligere hendelse på vedtak: $behandlingId", e)
            null
        }
    }

    fun hentSakStatistikkListeAlt(behandlingId: BigInteger): List<SakStatistikk> {
        try {
            val parameters = MapSqlParameterSource("behandlingId", behandlingId)
            val sql = "SELECT * FROM $SAK_STATISTIKK_TABLE WHERE $BEHANDLING_ID = :behandlingId"

            return namedParameterJdbcTemplate.query(sql, parameters, sakStatistikkRowMapper)
        } catch (e: Exception) {
            log.error("Kunne ikke hente sakStatistikkListeAlt", e)
            return emptyList()
        }
    }

    fun hentSakStatistikkListe(aktorId: String): List<SakStatistikk> {
        try {
            val parameters = MapSqlParameterSource("aktorId", aktorId)

            val sql = "SELECT * FROM $SAK_STATISTIKK_TABLE WHERE $AKTOR_ID = :aktorId"

            return namedParameterJdbcTemplate.query(sql, parameters, sakStatistikkRowMapper)
        } catch (e: Exception) {
            log.error("Kunne ikke hente sakStatistikkListe", e)
            return emptyList()
        }
    }

    fun hentSakStatistikkListe(sqlStatement: String, parameters: Map<String, Any>): List<SakStatistikk> {
        try {
            val parameters = MapSqlParameterSource(parameters)
            log.info("Prøver å hente sakStatistikkListe for å resende rader")
            return namedParameterJdbcTemplate.query(sqlStatement, parameters, sakStatistikkRowMapper)
        } catch (e: Exception) {
            log.error("Kunne ikke hente sakStatistikkListe til resending", e)
            return emptyList()
        }
    }

    private val sakStatistikkRowMapper: RowMapper<SakStatistikk> = RowMapper { rs, _ ->
        SakStatistikk(
            sekvensnummer = rs.getLong(SEKVENSNUMMER),
            aktorId = AktorId.of(rs.getString(AKTOR_ID)),
            oppfolgingPeriodeUUID = UUID.fromString(rs.getString(OPPFOLGING_PERIODE_UUID)),
            behandlingId = rs.getString(BEHANDLING_ID).toBigInteger(),
            relatertBehandlingId = rs.getString(RELATERT_BEHANDLING_ID)?.toBigInteger(),
            relatertFagsystem = rs.getString(RELATERT_FAGSYSTEM)?.let { Fagsystem.valueOf(it) },
            sakId = rs.getString(SAK_ID),
            mottattTid = rs.getTimestamp(MOTTATT_TID).toInstant(),
            registrertTid = rs.getTimestamp(REGISTRERT_TID).toInstant(),
            ferdigbehandletTid = rs.getTimestamp(FERDIGBEHANDLET_TID)?.toInstant(),
            endretTid = rs.getTimestamp(ENDRET_TID).toInstant(),
            sakYtelse = rs.getString(SAK_YTELSE),
            behandlingType = BehandlingType.valueOf(rs.getString(BEHANDLING_TYPE)),
            behandlingStatus = BehandlingStatus.valueOf(rs.getString(BEHANDLING_STATUS)),
            behandlingResultat = rs.getString(BEHANDLING_RESULTAT)?.let { BehandlingResultat.valueOf(it) },
            behandlingMetode = BehandlingMetode.valueOf(rs.getString(BEHANDLING_METODE)),
            innsatsgruppe = rs.getString(INNSATSGRUPPE)?.let { BehandlingResultat.valueOf(it) },
            hovedmal = rs.getString(HOVEDMAL)?.let { HovedmalNy.valueOf(it) },
            opprettetAv = rs.getString(OPPRETTET_AV),
            saksbehandler = rs.getString(SAKSBEHANDLER),
            ansvarligBeslutter = rs.getString(ANSVARLIG_BESLUTTER),
            ansvarligEnhet = EnhetId.of(rs.getString(ANSVARLIG_ENHET)),
            fagsystemNavn = Fagsystem.valueOf(rs.getString(FAGSYSTEM_NAVN)),
            fagsystemVersjon = rs.getString(FAGSYSTEM_VERSJON)
        )
    }
}

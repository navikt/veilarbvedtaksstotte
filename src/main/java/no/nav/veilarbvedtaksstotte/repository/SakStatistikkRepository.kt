package no.nav.veilarbvedtaksstotte.repository

import no.nav.veilarbvedtaksstotte.domain.statistikk.SakStatistikk
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigInteger
import java.util.*

@Repository
class SakStatistikkRepository(val jdbcTemplate: JdbcTemplate) {

    val namedParameterJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    val SAK_STATISTIKK_TABLE = "SAK_STATISTIKK"
    val AKTOR_ID = "AKTOR_ID"
    val OPPFOLGING_PERIODE_UUID = "OPPFOLGING_PERIODE_UUID"
    val BEHANDLING_ID = "BEHANDLING_ID"
    val BEHANDLING_UUID = "BEHANDLING_UUID"
    val RELATERT_BEHANDLING_ID = "RELATERT_BEHANDLING_ID"
    val RELATERT_FAGSYSTEM = "RELATERT_FAGSYSTEM"
    val SAK_ID = "SAK_ID"
    val MOTTATT_TID = "MOTTATT_TID"
    val REGISTRERT_TID = "REGISTRERT_TID"
    val FERDIGBEHANDLET_TID = "FERDIGBEHANDLET_TID"
    val ENDRET_TID = "ENDRET_TID"
    val TEKNISK_TID = "TEKNISK_TID"
    val SAK_YTELSE = "SAK_YTELSE"
    val BEHANDLING_TYPE = "BEHANDLING_TYPE"
    val BEHANDLING_STATUS = "BEHANDLING_STATUS"
    val BEHANDLING_RESULTAT = "BEHANDLING_RESULTAT"
    val BEHANDLING_METODE = "BEHANDLING_METODE"
    val INNSATSGRUPPE = "INNSATSGRUPPE"
    val HOVEDMAL = "HOVEDMAL"
    val OPPRETTET_AV = "OPPRETTET_AV"
    val SAKSBEHANDLER = "SAKSBEHANDLER"
    val ANSVARLIG_BESLUTTER = "ANSVARLIG_BESLUTTER"
    val ANSVARLIG_ENHET = "ANSVARLIG_ENHET"
    val AVSENDER = "AVSENDER"
    val VERSJON = "VERSJON"

    fun insertSakStatistikkRad(sakStatistikkRad: SakStatistikk) {
        val sql =
            """
                INSERT INTO $SAK_STATISTIKK_TABLE ($AKTOR_ID, $OPPFOLGING_PERIODE_UUID, $BEHANDLING_ID, $BEHANDLING_UUID, $RELATERT_BEHANDLING_ID,
                $RELATERT_FAGSYSTEM, $SAK_ID, $MOTTATT_TID, $REGISTRERT_TID, $FERDIGBEHANDLET_TID,
                $ENDRET_TID, $TEKNISK_TID, $SAK_YTELSE, $BEHANDLING_TYPE, $BEHANDLING_STATUS, 
                $BEHANDLING_RESULTAT, $BEHANDLING_METODE, $INNSATSGRUPPE, $HOVEDMAL, $OPPRETTET_AV, $SAKSBEHANDLER, $ANSVARLIG_BESLUTTER,
                $ANSVARLIG_ENHET, $AVSENDER, $VERSJON)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?, ?, ?)
                
            """

        jdbcTemplate.update(
            sql,
            sakStatistikkRad.behandlingId,
            sakStatistikkRad.aktorId,
            sakStatistikkRad.oppfolgingPeriodeUUID,
            sakStatistikkRad.behandlingUuid,
            sakStatistikkRad.relatertBehandlingId,
            sakStatistikkRad.relatertFagsystem,
            sakStatistikkRad.sakId,
            sakStatistikkRad.mottattTid,
            sakStatistikkRad.registrertTid,
            sakStatistikkRad.ferdigbehandletTid,
            sakStatistikkRad.endretTid,
            sakStatistikkRad.tekniskTid,
            sakStatistikkRad.sakYtelse,
            sakStatistikkRad.behandlingType,
            sakStatistikkRad.behandlingStatus,
            sakStatistikkRad.behandlingResultat,
            sakStatistikkRad.behandlingMetode,
            sakStatistikkRad.innsatsgruppe,
            sakStatistikkRad.hovedmal,
            sakStatistikkRad.opprettetAv,
            sakStatistikkRad.saksbehandler,
            sakStatistikkRad.ansvarligBeslutter,
            sakStatistikkRad.ansvarligEnhet,
            sakStatistikkRad.avsender,
            sakStatistikkRad.versjon
        )
    }
    fun hentSakStatistikkListeAlt(behandlingId: BigInteger): List<SakStatistikk> {
        val parameters = MapSqlParameterSource("behandlingId", behandlingId)
        val sql = "SELECT * FROM $SAK_STATISTIKK_TABLE WHERE $BEHANDLING_ID = :behandlingId"

        return namedParameterJdbcTemplate.query(sql, parameters, sakStatistikkRowMapper)
    }
    fun hentSakStatistikkListe(aktorId: String): List<SakStatistikk> {
        val parameters = MapSqlParameterSource("aktorId", aktorId)

        val sql = "SELECT * FROM $SAK_STATISTIKK_TABLE WHERE $AKTOR_ID = :aktorId"

        return namedParameterJdbcTemplate.query(sql, parameters, sakStatistikkRowMapper)
    }
    /*
    fun insertSakStatistikkRadUtkast(behandlingId: String, aktorId: String, opprettetAv: String, ansvarligEnhet: String) {
        val parameters = MapSqlParameterSource()
            .addValue("behandlingId", behandlingId.toBigInteger(), Types.BIGINT)
            .addValue("aktorId", aktorId, Types.VARCHAR)
            .addValue("opprettetAv", opprettetAv, Types.VARCHAR)
            .addValue("ansvarligEnhet", ansvarligEnhet, Types.VARCHAR)
            .addValue("avsender", "Oppfølgingsvedtak § 14 a", Types.VARCHAR)
            .addValue("versjon", "Dockerimage_tag_1", Types.VARCHAR)

        val sql = """INSERT INTO $SAK_STATISTIKK_TABLE ($BEHANDLING_ID, $AKTOR_ID, $OPPRETTET_AV, $ANSVARLIG_ENHET, $AVSENDER, $VERSJON) 
                 VALUES (:behandlingId, :aktorId, :opprettetAv, :ansvarligEnhet, :avsender, :versjon)"""
        jdbcTemplate.update(sql, parameters)
    }
*/

    private val sakStatistikkRowMapper: RowMapper<SakStatistikk> = RowMapper { rs, _ ->
        SakStatistikk(
            aktorId = rs.getString(AKTOR_ID),
            oppfolgingPeriodeUUID = rs.getString(OPPFOLGING_PERIODE_UUID)?.let { UUID.fromString(it) },
            behandlingId = rs.getBigDecimal(BEHANDLING_ID).toBigInteger(),
            behandlingUuid = rs.getString(BEHANDLING_UUID)?.let { UUID.fromString(it) },
            relatertBehandlingId = rs.getBigDecimal(RELATERT_BEHANDLING_ID)?.toBigInteger(),
            relatertFagsystem = rs.getString(RELATERT_FAGSYSTEM),
            sakId = rs.getString(SAK_ID),
            mottattTid = rs.getTimestamp(MOTTATT_TID).toLocalDateTime(),
            registrertTid = rs.getTimestamp(REGISTRERT_TID).toLocalDateTime(),
            ferdigbehandletTid = rs.getTimestamp(FERDIGBEHANDLET_TID)?.toLocalDateTime(),
            endretTid = rs.getTimestamp(ENDRET_TID)?.toLocalDateTime(),
            tekniskTid = rs.getTimestamp(TEKNISK_TID)?.toLocalDateTime(),
            sakYtelse = rs.getString(SAK_YTELSE),
            behandlingType = rs.getString(BEHANDLING_TYPE),
            behandlingStatus = rs.getString(BEHANDLING_STATUS),
            behandlingResultat = rs.getString(BEHANDLING_RESULTAT),
            behandlingMetode = rs.getString(BEHANDLING_METODE),
            innsatsgruppe = rs.getString(INNSATSGRUPPE),
            hovedmal = rs.getString(HOVEDMAL),
            opprettetAv = rs.getString(OPPRETTET_AV),
            saksbehandler = rs.getString(SAKSBEHANDLER),
            ansvarligBeslutter = rs.getString(ANSVARLIG_BESLUTTER),
            ansvarligEnhet = rs.getString(ANSVARLIG_ENHET),
            avsender = rs.getString(AVSENDER),
            versjon = rs.getString(VERSJON)
        )
    }
}

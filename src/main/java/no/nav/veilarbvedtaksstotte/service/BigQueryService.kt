package no.nav.veilarbvedtaksstotte.service

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.TableId
import no.nav.veilarbvedtaksstotte.domain.statistikk.SakStatistikk
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter


@Service
class BigQueryService(@Value("\${gcp.projectId}") val projectId: String) {
    private final val DATASET_NAME = "sak_statistikk_dataset_test"
    private final val TABLE_NAME = "14a_vedtak_tabell"
    val vedtakStatistikkTable = TableId.of(DATASET_NAME, TABLE_NAME)

    val bigQuery = BigQueryOptions.newBuilder().setProjectId(projectId).build().service
    val log = LoggerFactory.getLogger(BigQueryService::class.java)

    fun TableId.insertRequest(row: Map<String, Any?>): InsertAllRequest {
        return InsertAllRequest.newBuilder(this).addRow(row).build()
    }

    fun logEvent(sakStatistikk: SakStatistikk) {
        val vedtakStatistikkRow = mapOf(
            "behandling_id" to sakStatistikk.behandlingId.toInt(),
            "aktor_id" to sakStatistikk.aktorId,
            "oppfolging_periode_uuid" to sakStatistikk.oppfolgingPeriodeUUID.toString(),
            "relatert_behandling_id" to sakStatistikk.relatertBehandlingId?.toInt(),
            "relatert_fagsystem" to sakStatistikk.relatertFagsystem?.name,
            "sak_id" to sakStatistikk.sakId,
            "mottatt_tid" to sakStatistikk.mottattTid.toString(),
            "registrert_tid" to sakStatistikk.registrertTid.toString(),
            "ferdigbehandlet_tid" to sakStatistikk.ferdigbehandletTid.toString(),
            "endret_tid" to sakStatistikk.endretTid.toString(),
            "teknisk_tid" to sakStatistikk.tekniskTid.toString(),
            "sak_ytelse" to sakStatistikk.sakYtelse,
            "behandling_type" to sakStatistikk.behandlingType.name,
            "behandling_status" to sakStatistikk.behandlingStatus.name,
            "behandling_resultat" to sakStatistikk.behandlingResultat?.name,
            "behandling_metode" to sakStatistikk.behandlingMetode.name,
            "innsatsgruppe" to sakStatistikk.innsatsgruppe?.name,
            "hovedmal" to sakStatistikk.hovedmal?.name,
            "opprettet_av" to sakStatistikk.opprettetAv,
            "saksbehandler" to sakStatistikk.saksbehandler,
            "ansvarlig_beslutter" to sakStatistikk.ansvarligBeslutter,
            "ansvarlig_enhet" to sakStatistikk.ansvarligEnhet?.get(),
            "avsender" to sakStatistikk.avsender.name,
            "versjon" to sakStatistikk.versjon,
        )
        val moteEvent = vedtakStatistikkTable.insertRequest(vedtakStatistikkRow)
        insertWhileToleratingErrors(moteEvent)
    }
    private fun insertWhileToleratingErrors(insertRequest: InsertAllRequest) {
        runCatching {
            val response = bigQuery.insertAll(insertRequest)
            val errors = response.insertErrors
            if (errors.isNotEmpty()) {
                log.error("Error inserting bigquery rows {}", errors)
            }
        }.onFailure {
            log.error("BigQuery error", it)
        }
    }
}
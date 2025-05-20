package no.nav.veilarbvedtaksstotte.service

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.TableId
import no.nav.veilarbvedtaksstotte.domain.statistikk.SakStatistikk
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class BigQueryService(
    @Value("\${gcp.bq.datasetName}") val datasetName: String,
    @Value("\${gcp.bq.tableName}") val tableName: String,
    val bigQuery: BigQuery
) {

    val vedtakStatistikkTable: TableId = TableId.of(datasetName, tableName)

    val log: Logger = LoggerFactory.getLogger(BigQueryService::class.java)

    fun TableId.insertRequest(rows: List<Map<String, Any?>>): InsertAllRequest {
        val request = InsertAllRequest.newBuilder(this)
        rows.forEach { row -> request.addRow(row) }
        return request.build()
    }

    fun logEvent(sakStatistikk: SakStatistikk) {
        logEvent(listOf(sakStatistikk))
    }

    fun logEvent(sakStatistikk: List<SakStatistikk>) {
        if (sakStatistikk.isEmpty()) {
            log.warn("Ingen statistikkrader å sende til BigQuery")
            return
        }

        val vedtakStatistikkRader = sakStatistikk.map {
            mapOf(
                "sekvensnummer" to it.sekvensnummer?.toBigInteger(),
                "behandling_id" to it.behandlingId?.toInt(),
                "aktor_id" to it.aktorId?.get(),
                "oppfolging_periode_uuid" to it.oppfolgingPeriodeUUID.toString(),
                "relatert_behandling_id" to it.relatertBehandlingId?.toInt(),
                "relatert_fagsystem" to it.relatertFagsystem?.name,
                "sak_id" to it.sakId,
                "mottatt_tid" to it.mottattTid.toString(),
                "registrert_tid" to it.registrertTid.toString(),
                "ferdigbehandlet_tid" to it.ferdigbehandletTid?.toString(),
                "endret_tid" to it.endretTid.toString(),
                "sak_ytelse" to it.sakYtelse,
                "behandling_type" to it.behandlingType?.name,
                "behandling_status" to it.behandlingStatus?.name,
                "behandling_resultat" to it.behandlingResultat?.name,
                "behandling_metode" to it.behandlingMetode?.name,
                "innsatsgruppe" to it.innsatsgruppe?.name,
                "hovedmal" to it.hovedmal?.name,
                "opprettet_av" to it.opprettetAv,
                "saksbehandler" to it.saksbehandler,
                "ansvarlig_beslutter" to it.ansvarligBeslutter,
                "ansvarlig_enhet" to it.ansvarligEnhet?.get(),
                "fagsystem_navn" to it.fagsystemNavn.name,
                "fagsystem_versjon" to it.fagsystemVersjon,
            ).filter { rad -> rad.value != null }
        }
        val vedtak14aEvent = vedtakStatistikkTable.insertRequest(vedtakStatistikkRader)
        insertWhileToleratingErrors(vedtak14aEvent)
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
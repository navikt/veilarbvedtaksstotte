package no.nav.veilarbvedtaksstotte.config

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryException
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.TableDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import no.nav.veilarbvedtaksstotte.domain.statistikk.vedtakStatistikkSchema
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BigQueryConfiguration(
    @Value("\${gcp.projectId}") val projectId: String,
    @Value("\${gcp.bq.datasetName}") val datasetName: String,
    @Value("\${gcp.bq.tableName}") val tableName: String
) {

    @Bean
    fun bigQueryConfig(): BigQuery {
        val bigQuery: BigQuery = BigQueryOptions.newBuilder().setProjectId(projectId).build().service
        val vedtakStatistikkTable: TableId = TableId.of(datasetName, tableName)
        val schema = vedtakStatistikkSchema

        val log: Logger = LoggerFactory.getLogger(this::class.java)

        val table = bigQuery.getTable(vedtakStatistikkTable)

        if (table == null) { // table will be null if it is not found and setThrowNotFound is not set to `true`
            try {
                val tableDefinition: TableDefinition = StandardTableDefinition.of(schema)
                val tableInfo: TableInfo = TableInfo.newBuilder(vedtakStatistikkTable, tableDefinition).build()

                bigQuery.create(tableInfo)

                log.info("BigQuery-tabellen ble opprettet i {} {}.", datasetName, tableName)
            } catch (e: BigQueryException) {
                log.error("BigQuery-tabellen ble ikke opprettet.", e)
            }
        }

        return bigQuery
    }
}
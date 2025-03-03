package no.nav.veilarbvedtaksstotte.domain.statistikk

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName

val vedtakStatistikkSchema: Schema = Schema.of(
    Field.of("sekvensnummer", StandardSQLTypeName.INT64),
    Field.of("behandling_id", StandardSQLTypeName.INT64),
    Field.of("relatert_behandling_id", StandardSQLTypeName.INT64),
    Field.of("relatert_fagsystem", StandardSQLTypeName.STRING),
    Field.of("sak_id", StandardSQLTypeName.STRING),
    Field.of("aktor_id", StandardSQLTypeName.STRING),
    Field.of("mottatt_tid", StandardSQLTypeName.TIMESTAMP),
    Field.of("registrert_tid", StandardSQLTypeName.TIMESTAMP),
    Field.of("ferdigbehandlet_tid", StandardSQLTypeName.TIMESTAMP),
    Field.of("endret_tid", StandardSQLTypeName.TIMESTAMP),
    Field.newBuilder("teknisk_tid", StandardSQLTypeName.TIMESTAMP)
        .setDefaultValueExpression("CURRENT_TIMESTAMP()") // Gjør at BigQuery setter verdien når raden blir lagt til
        .build(),
    Field.of("sak_ytelse", StandardSQLTypeName.STRING),
    Field.of("behandling_type", StandardSQLTypeName.STRING),
    Field.of("behandling_status", StandardSQLTypeName.STRING),
    Field.of("behandling_resultat", StandardSQLTypeName.STRING),
    Field.of("behandling_metode", StandardSQLTypeName.STRING),
    Field.of("opprettet_av", StandardSQLTypeName.STRING),
    Field.of("saksbehandler", StandardSQLTypeName.STRING),
    Field.of("ansvarlig_beslutter", StandardSQLTypeName.STRING),
    Field.of("ansvarlig_enhet", StandardSQLTypeName.STRING),
    Field.of("fagsystem_navn", StandardSQLTypeName.STRING),
    Field.of("fagsystem_versjon", StandardSQLTypeName.STRING),
    Field.of("oppfolging_periode_uuid", StandardSQLTypeName.STRING),
    Field.of("innsatsgruppe", StandardSQLTypeName.STRING),
    Field.of("hovedmal", StandardSQLTypeName.STRING),
)

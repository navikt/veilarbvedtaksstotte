package no.nav.veilarbvedtaksstotte.client.dokdistfordeling.dto

data class DistribuerJournalpostDTO(
    val journalpostId: String,
    val bestillendeFagsystem: String,
    val dokumentProdApp: String,
    val distribusjonstype: String,
    val distribusjonstidspunkt: String
)

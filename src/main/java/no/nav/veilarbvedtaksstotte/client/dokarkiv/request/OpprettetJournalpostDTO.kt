package no.nav.veilarbvedtaksstotte.client.dokarkiv.request

data class OpprettetJournalpostDTO(
    val journalpostId: String,
    val journalpostferdigstilt: Boolean,
    val dokumenter: List<DokumentInfoId>
) {

    data class DokumentInfoId(val dokumentInfoId: String)
}


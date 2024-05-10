package no.nav.veilarbvedtaksstotte.client.dokarkiv.request

data class QueryVariables(
    val journalPostId: String
)

enum class BrukerIdType {
    FNR
}

data class BrukerId(
    val id: String,
    val type: BrukerIdType,
)

data class DokumentOversiktBrukerVariables(
    val brukerId: BrukerId,
    val tema: String,
    val foerste: Int
)
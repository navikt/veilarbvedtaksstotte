package no.nav.veilarbvedtaksstotte.client.dokarkiv

interface DokarkivClient {
    fun opprettJournalpost(request: OpprettJournalpostDTO): OpprettetJournalpostDTO
}

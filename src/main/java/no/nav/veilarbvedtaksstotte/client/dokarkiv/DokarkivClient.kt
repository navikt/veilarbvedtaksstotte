package no.nav.veilarbvedtaksstotte.client.dokarkiv

interface DokarkivClient {
    fun opprettJournalpost(opprettJournalpostDTO: OpprettJournalpostDTO): OpprettetJournalpostDTO
}

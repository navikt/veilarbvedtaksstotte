package no.nav.veilarbvedtaksstotte.client.dokdistfordeling

interface DokdistribusjonClient {
    fun distribuerJournalpost(request: DistribuerJournalpostDTO): DistribuerJournalpostResponsDTO
}

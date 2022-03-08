package no.nav.veilarbvedtaksstotte.client.dokdistfordeling

import no.nav.common.health.HealthCheck

interface DokdistribusjonClient : HealthCheck {
    fun distribuerJournalpost(dto: DistribuerJournalpostDTO): DistribuerJournalpostResponsDTO?
}

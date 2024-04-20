package no.nav.veilarbvedtaksstotte.client.dokdistfordeling

import no.nav.common.health.HealthCheck
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.dto.DistribuerJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.dto.DistribuerJournalpostResponsDTO

interface DokdistribusjonClient : HealthCheck {
    fun distribuerJournalpost(dto: DistribuerJournalpostDTO): DistribuerJournalpostResponsDTO?
}

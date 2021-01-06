package no.nav.veilarbvedtaksstotte.client.dokarkiv

import no.nav.common.health.HealthCheck

interface DokarkivClient : HealthCheck {
    fun opprettJournalpost(opprettJournalpostDTO: OpprettJournalpostDTO): OpprettetJournalpostDTO
}

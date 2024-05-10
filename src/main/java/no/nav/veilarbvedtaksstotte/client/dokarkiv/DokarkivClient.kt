package no.nav.veilarbvedtaksstotte.client.dokarkiv

import no.nav.common.health.HealthCheck
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.OpprettJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.OpprettetJournalpostDTO

interface DokarkivClient : HealthCheck {
    fun opprettJournalpost(opprettJournalpostDTO: OpprettJournalpostDTO): OpprettetJournalpostDTO
}

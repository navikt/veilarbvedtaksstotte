package no.nav.veilarbvedtaksstotte.client.dokdistkanal

import no.nav.common.health.HealthCheck
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.dokdistkanal.dto.BestemDistribusjonskanalResponseDTO

interface DokdistkanalClient: HealthCheck {
    fun bestemDistribusjonskanal(brukerFnr: Fnr): BestemDistribusjonskanalResponseDTO
}

package no.nav.veilarbvedtaksstotte.client.aiaBackend

import no.nav.common.health.HealthCheck
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EgenvurderingResponseDTO
import no.nav.veilarbvedtaksstotte.client.aiaBackend.request.EgenvurderingForPersonRequest

interface AiaBackendClient : HealthCheck {
    fun hentEgenvurdering(egenvurderingForPersonRequest: EgenvurderingForPersonRequest): EgenvurderingResponseDTO?

}

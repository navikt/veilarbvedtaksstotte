package no.nav.veilarbvedtaksstotte.client.aiaBackend

import no.nav.common.health.HealthCheck
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EgenvurderingResponseDTO
import no.nav.veilarbvedtaksstotte.client.aiaBackend.request.EndringIRegistreringdataRequest

interface AiaBackendClient : HealthCheck {
    fun hentEgenvurdering(egenvurderingForPersonDTO: EgenvurderingForPersonDTO): EgenvurderingResponseDTO?

    fun hentEndringIRegistreringdata(endringIRegistreringdataRequest: EndringIRegistreringdataRequest): EndringIRegistreringsdataResponse?
}

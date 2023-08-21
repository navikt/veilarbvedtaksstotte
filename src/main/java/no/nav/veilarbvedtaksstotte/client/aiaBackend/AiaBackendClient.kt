package no.nav.veilarbvedtaksstotte.client.aiaBackend

import no.nav.common.health.HealthCheck

interface AiaBackendClient : HealthCheck {
    fun hentEgenvurdering(egenvurderingForPersonDTO: EgenvurderingForPersonDTO): EgenvurderingResponseDTO?

    fun hentEndringIRegistreringdata(endringIRegistreringdataRequest: EndringIRegistreringdataRequest): EndringIRegistreringsdataResponse?
}

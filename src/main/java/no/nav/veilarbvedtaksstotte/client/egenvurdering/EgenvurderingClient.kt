package no.nav.veilarbvedtaksstotte.client.egenvurdering

import no.nav.common.health.HealthCheck

interface EgenvurderingClient : HealthCheck {
    fun hentEgenvurdering(egenvurderingForPersonDTO: EgenvurderingForPersonDTO): EgenvurderingResponseDTO?
}

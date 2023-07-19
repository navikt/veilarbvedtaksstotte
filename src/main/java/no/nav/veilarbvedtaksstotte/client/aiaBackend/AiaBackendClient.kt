package no.nav.veilarbvedtaksstotte.client.aiaBackend

import no.nav.common.health.HealthCheck
import no.nav.veilarbvedtaksstotte.client.egenvurdering.EgenvurderingForPersonDTO
import no.nav.veilarbvedtaksstotte.client.egenvurdering.EgenvurderingResponseDTO
import no.nav.veilarbvedtaksstotte.client.registrering.endring.EndringIRegistreringdataRequestDTO

interface AiaBackendClient : HealthCheck {

	fun hentEgenvurdering(egenvurderingForPersonDTO: EgenvurderingForPersonDTO): EgenvurderingResponseDTO?
	fun hentEndringIRegistreringsdata(endringIRegistreringdataRequestDTO: EndringIRegistreringdataRequestDTO)
}

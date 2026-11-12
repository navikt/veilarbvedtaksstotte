package no.nav.veilarbvedtaksstotte.client.veilarbportefolje

import no.nav.common.health.HealthCheck
import no.nav.common.types.identer.NorskIdent
import no.nav.veilarbvedtaksstotte.client.veilarbportefolje.dto.EtiketterForBrukerDTO

interface VeilarbportefoljeClient : HealthCheck {
    fun hentEtiketter(fnrListe: List<NorskIdent>): List<EtiketterForBrukerDTO>
}

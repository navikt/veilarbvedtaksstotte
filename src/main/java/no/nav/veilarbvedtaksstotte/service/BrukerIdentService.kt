package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.BrukerIdenter
import org.springframework.stereotype.Service

@Service
class BrukerIdentService(val aktorOppslagClient: AktorOppslagClient) {

    fun hentIdenter(fnr: Fnr): BrukerIdenter {
        // TODO: slå opp historiskeFnr. Bruk PDL
        return BrukerIdenter(
            fnr = fnr,
            aktorId = aktorOppslagClient.hentAktorId(fnr),
            historiskeFnr = listOf()
        )
    }
}

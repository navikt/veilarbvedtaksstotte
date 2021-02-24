package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EksternBrukerId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.BrukerIdenter
import org.springframework.stereotype.Service

@Service
class BrukerIdentService(val aktorOppslagClient: AktorOppslagClient) {
    // TODO: slå opp historiskeFnr. Bruk PDL
    fun hentIdenter(brukerId: EksternBrukerId): BrukerIdenter {
        return when (brukerId) {
            is AktorId ->
                BrukerIdenter(
                    fnr = aktorOppslagClient.hentFnr(brukerId),
                    aktorId = brukerId,
                    historiskeFnr = listOf()
                )
            is Fnr ->
                return BrukerIdenter(
                    fnr = brukerId,
                    aktorId = aktorOppslagClient.hentAktorId(brukerId),
                    historiskeFnr = listOf()
                )
            else -> throw IllegalStateException("Ukjent EksternBrukerId " + brukerId.type().name)
        }

    }
}

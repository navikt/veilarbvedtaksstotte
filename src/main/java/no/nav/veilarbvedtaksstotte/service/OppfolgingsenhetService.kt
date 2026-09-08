package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class OppfolgingsenhetService(
    private val veilarboppfolgingClient: VeilarboppfolgingClient
) {
    fun hentOppfolgingsenhet(fnr: Fnr): Optional<EnhetId> {
        return veilarboppfolgingClient.hentOppfolgingsenhet(fnr)
            .flatMap { dto ->
                Optional.ofNullable(dto.enhetId)
                    .filter { it.isNotBlank() }
                    .map { EnhetId.of(it) }
        }
    }
}

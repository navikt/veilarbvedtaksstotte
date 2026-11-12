package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.NorskIdent
import no.nav.veilarbvedtaksstotte.client.veilarbportefolje.VeilarbportefoljeClient
import no.nav.veilarbvedtaksstotte.client.veilarbportefolje.dto.Etiketter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EtiketterService(
    private val veilarbportefoljeClient: VeilarbportefoljeClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun hentEtiketter(fnrListe: List<String>): Map<String, Etiketter> {
        val fnrSomSkalHentes = fnrListe.filter { it.isNotBlank() }.distinct()
        if (fnrSomSkalHentes.isEmpty()) {
            return emptyMap()
        }

        return try {
            veilarbportefoljeClient.hentEtiketter(fnrSomSkalHentes.map { NorskIdent.of(it) })
                .associate { it.fnr.get() to it.etiketter }
        } catch (e: Exception) {
            log.warn("Klarte ikke å hente etiketter fra veilarbportefolje", e)
            emptyMap()
        }
    }
}

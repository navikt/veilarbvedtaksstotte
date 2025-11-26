package no.nav.veilarbvedtaksstotte.controller

import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NorskIdent
import no.nav.common.utils.EnvironmentUtils
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.ArbeidssoekerregisteretApiOppslagV2Client
import no.nav.veilarbvedtaksstotte.service.AuthService
import org.openapitools.model.AggregertPeriode
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/dummy")
class DummyController(
    val arbeidssoekerregisteretApiOppslagV2Client: ArbeidssoekerregisteretApiOppslagV2Client,
    val authService: AuthService
) {
    @PostMapping("/egenvurdering")
    fun hentEgenvurdering(@RequestBody norskIdent: NorskIdent): AggregertPeriode {
        if (EnvironmentUtils.isProduction().orElse(false)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Dette endepunktet er ikke tilgjengelig i produksjonsmiljøet")
        }
        if(!authService.erInternBruker()) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Kun interne brukere har tilgang til dette endepunktet")
        }
        authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.LESE, Fnr.of(norskIdent.get()))

        try {
            return arbeidssoekerregisteretApiOppslagV2Client.hentEgenvurdering(norskIdent)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}
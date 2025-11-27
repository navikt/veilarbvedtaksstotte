package no.nav.veilarbvedtaksstotte.controller

import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NorskIdent
import no.nav.common.utils.EnvironmentUtils
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.ArbeidssoekerregisteretApiOppslagV2Client
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.AggregertPeriode
import no.nav.veilarbvedtaksstotte.service.AuthService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    val log: Logger = LoggerFactory.getLogger(DummyController::class.java)

    @PostMapping("/egenvurdering")
    fun hentEgenvurdering(@RequestBody norskIdent: NorskIdent): AggregertPeriode {
        if (EnvironmentUtils.isProduction().orElse(false)) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Dette endepunktet er ikke tilgjengelig i produksjonsmiljøet"
            )
        }
        if (!authService.erInternBruker()) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Kun interne brukere har tilgang til dette endepunktet")
        }
        authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.LESE, Fnr.of(norskIdent.get()))

        try {
            return arbeidssoekerregisteretApiOppslagV2Client.hentEgenvurdering(norskIdent)
        } catch (e: Exception) {
            log.warn("Feil ved henting av egenvurdering for bruker", e)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}

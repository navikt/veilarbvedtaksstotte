package no.nav.veilarbvedtaksstotte.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.controller.dto.Siste14aVedtakDTO
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.Siste14aVedtakService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api")
@Tag(
    name = "Siste § 14 a-vedtak",
    description = "Funksjonalitet knyttet til siste § 14 a-vedtak."
)
class Siste14aVedtakController(
    val authService: AuthService,
    val siste14aVedtakService: Siste14aVedtakService
    ) {

    @Deprecated("Ikke lenger i bruk pga personvernstiltak", ReplaceWith("v2 av samme endepunkt"))
    @GetMapping("/siste-14a-vedtak")
    fun hentSiste14aVedtak(@RequestParam("fnr") fnr: Fnr): Siste14aVedtakDTO? {
        sjekkTilgang(fnr)

        return siste14aVedtakService.siste14aVedtak(fnr)
            ?.let { Siste14aVedtakDTO.fraSiste14aVedtak(it) }
    }

    private fun sjekkTilgang(fnr: Fnr) {
        if (authService.erSystemBruker()) {
            if (!authService.harSystemTilSystemTilgangMedEkstraRolle("siste-14a-vedtak")) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        } else {
            authService.sjekkVeilederTilgangTilBruker(tilgangType = TilgangType.LESE, fnr = fnr)
        }
    }
}

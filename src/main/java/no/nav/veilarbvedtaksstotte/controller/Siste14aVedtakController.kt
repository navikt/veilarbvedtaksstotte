package no.nav.veilarbvedtaksstotte.controller

import no.nav.common.types.identer.Fnr
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
            authService.sjekkVeilederTilgangTilBruker(fnr)
        }
    }
}

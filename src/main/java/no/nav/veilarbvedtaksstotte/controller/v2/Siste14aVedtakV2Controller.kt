package no.nav.veilarbvedtaksstotte.controller.v2

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.controller.dto.Siste14aVedtakDTO
import no.nav.veilarbvedtaksstotte.controller.v2.dto.Siste14aVedtakRequest
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.Siste14aVedtakService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v2")
class Siste14aVedtakV2Controller(
    val authService: AuthService,
    val siste14aVedtakService: Siste14aVedtakService
) {

    @PostMapping("/hent-siste-14a-vedtak")
    fun hentSiste14aVedtak(@RequestBody siste14aVedtakRequest: Siste14aVedtakRequest): Siste14aVedtakDTO? {
        sjekkTilgang(siste14aVedtakRequest.fnr)

        return siste14aVedtakService.siste14aVedtak(siste14aVedtakRequest.fnr)
            ?.let { Siste14aVedtakDTO.fraSiste14aVedtak(it) }
    }

    private fun sjekkTilgang(fnr: Fnr) {
        if (authService.erSystemBruker()) {
            if (!authService.harSystemTilSystemTilgangMedEkstraRolle("siste-14a-vedtak")) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN)
            }
        } else {
            authService.sjekkTilgangTilBruker(fnr)
        }
    }
}

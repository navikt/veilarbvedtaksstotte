package no.nav.veilarbvedtaksstotte.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.common.types.identer.Fnr
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
class Siste14aVedtakController {

    @Deprecated("Ikke lenger i bruk pga personvernstiltak", ReplaceWith("/api/v2/hent-siste-14a-vedtak"))
    @GetMapping("/siste-14a-vedtak")
    fun hentSiste14aVedtak(@RequestParam("fnr") fnr: Fnr) {
        throw ResponseStatusException(HttpStatus.GONE)
    }
}

package no.nav.veilarbvedtaksstotte.klagebehandling.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.dto.FormkravRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.dto.OpprettKlageRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.service.KlageService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api")
@Tag(
    name = "Klage på § 14 a-vedtak",
    description = "Funksjonalitet knyttet til klage på § 14 a-vedtak."
)
class KlageController(val klageService: KlageService) {

    //TODO: Tilgangskontroll ? hva skal vi ha?

    @PostMapping("/klagebehandling/opprett-klage")
    fun opprettKlagePa14aVedtak(@Valid @RequestBody opprettKlageRequest: OpprettKlageRequest) {
        return klageService.opprettKlageBehandling(opprettKlageRequest)

    }

    @PostMapping("/klagebehandling/formkrav")
    fun oppdaterFormkrav(@Valid @RequestBody formkravrequest: FormkravRequest) {
        return klageService.oppdaterFormkrav(formkravrequest)
    }

}

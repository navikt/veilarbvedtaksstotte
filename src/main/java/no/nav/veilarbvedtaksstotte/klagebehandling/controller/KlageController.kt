package no.nav.veilarbvedtaksstotte.klagebehandling.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.KlageBehandling
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.dto.FormkravRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.dto.KlageRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.dto.OpprettKlageRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.service.KlageService
import org.springframework.http.ResponseEntity
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

    //TODO: Tilgangskontroll - hva skal vi ha?

    @PostMapping("/klagebehandling/opprett-klage")
    fun opprettKlagePa14aVedtak(@Valid @RequestBody opprettKlageRequest: OpprettKlageRequest) {
        return klageService.opprettKlageBehandling(opprettKlageRequest)
    }

    @PostMapping("/klagebehandling/formkrav")
    fun oppdaterFormkrav(@Valid @RequestBody formkravrequest: FormkravRequest) {
        return klageService.oppdaterFormkrav(formkravrequest)
    }

    @PostMapping("/klagebehandling/hent-klage")
    fun hentKlage(@Valid @RequestBody klageRequest: KlageRequest): ResponseEntity<KlageBehandling?>? {
        val klage = klageService.hentKlage(klageRequest)
        return if (klage != null) {
            ResponseEntity.ok(klage)
        } else {
            ResponseEntity.noContent().build()
        }
    }

    // Lager et endepunkt for å sende en klage til kabal så vi lettere kan teste.
    // Kan fjernes når vi har fått på plass backend-logikken for å sende klagen til kabal når bruker ikke får medhold og saken går til KA.
    @PostMapping("/klagebehandling/send-klage-til-kabal")
    fun sendKlageTilKabal(@Valid @RequestBody klageRequest: KlageRequest) {
        return klageService.sendKlageTilKabal(klageRequest)
    }

}

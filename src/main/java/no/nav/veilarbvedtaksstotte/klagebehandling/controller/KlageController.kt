package no.nav.veilarbvedtaksstotte.klagebehandling.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EksternBrukerId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils.isDevelopment
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.dto.FormkravRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.dto.KlageRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.dto.OpprettKlageRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.KlageBehandling
import no.nav.veilarbvedtaksstotte.klagebehandling.service.KlageService
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.AuthService
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
class KlageController(
    val klageService: KlageService,
    val authService: AuthService,
    val vedtakRepository: VedtaksstotteRepository
) {

    //TODO: Tilgangskontroll - hva skal vi ha?

    @PostMapping("/klagebehandling/opprett-klage")
    fun opprettKlagePa14aVedtak(@Valid @RequestBody opprettKlageRequest: OpprettKlageRequest) {
        validerMiljo()
        validerTilganger(TilgangType.SKRIVE, authService, opprettKlageRequest.fnr)
        return klageService.opprettKlageBehandling(opprettKlageRequest)
    }

    @PostMapping("/klagebehandling/formkrav")
    fun oppdaterFormkrav(@Valid @RequestBody formkravrequest: FormkravRequest) {
        validerMiljo()
        validerTilganger(
            TilgangType.SKRIVE,
            authService,
            hentAktorId(formkravrequest.vedtakId, vedtakRepository)
        )
        return klageService.oppdaterFormkrav(formkravrequest)
    }

    @PostMapping("/klagebehandling/hent-klage")
    fun hentKlage(@Valid @RequestBody klageRequest: KlageRequest): ResponseEntity<KlageBehandling?>? {
        validerMiljo()
        validerTilganger(
            TilgangType.SKRIVE,
            authService,
            hentAktorId(klageRequest.vedtakId, vedtakRepository)
        )
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
        validerMiljo()
        validerTilganger(
            tilgangType = TilgangType.SKRIVE,
            authService,
            hentAktorId(klageRequest.vedtakId, vedtakRepository)
        )
        return klageService.sendKlageTilKabal(klageRequest)
    }

    companion object {
        @JvmStatic
        internal fun validerMiljo() {
            require(isDevelopment().orElse(false)) {
                "Funksjonaliteten er ikke tilgjengelig i dette miljøet."
            }
        }

        internal fun validerTilganger(
            tilgangType: TilgangType,
            authService: AuthService,
            eksternBrukerId: EksternBrukerId
        ) {
            if (eksternBrukerId is Fnr) {
                authService.sjekkTilgangTilBrukerOgEnhet(tilgangType, fnr = eksternBrukerId)
                return
            }

            if (eksternBrukerId is AktorId) {
                authService.sjekkTilgangTilBrukerOgEnhet(tilgangType, aktorId = eksternBrukerId)
                return
            }
        }

        internal fun hentAktorId(vedtakId: Long, vedtakRepository: VedtaksstotteRepository): AktorId {
            return requireNotNull(vedtakRepository.hentVedtak(vedtakId)) {
                "Fant ingen vedtak med id $vedtakId."
            }.aktorId.let { AktorId.of(it) }
        }
    }
}

package no.nav.veilarbvedtaksstotte.klagebehandling.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EksternBrukerId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils.isDevelopment
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.KlageController.Mapper.tilKlageFormkravData
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.KlageController.Mapper.tilKlageInitiellData
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.KlageFormkravData
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.KlageInitiellData
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.KlageBehandling
import no.nav.veilarbvedtaksstotte.klagebehandling.service.KlageService
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException


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
        return klageService.startNyKlagebehandling(tilKlageInitiellData(opprettKlageRequest))
    }

    @PostMapping("/klagebehandling/formkrav")
    fun oppdaterFormkrav(@Valid @RequestBody formkravrequest: OppdaterFormkravRequest) {
        validerMiljo()
        validerTilganger(
            TilgangType.SKRIVE,
            authService,
            hentAktorId(formkravrequest.vedtakId, vedtakRepository)
        )
        return klageService.oppdaterFormkrav(formkravrequest.vedtakId, tilKlageFormkravData(formkravrequest))
    }

    @PostMapping("/klagebehandling/hent-klage")
    fun hentKlage(@Valid @RequestBody hentKlageRequest: HentKlageRequest): ResponseEntity<KlageBehandling?>? {
        validerMiljo()
        validerTilganger(
            TilgangType.SKRIVE,
            authService,
            hentAktorId(hentKlageRequest.vedtakId, vedtakRepository)
        )
        val klage = klageService.hentKlage(hentKlageRequest.vedtakId)
        return if (klage != null) {
            ResponseEntity.ok(klage)
        } else {
            ResponseEntity.noContent().build()
        }
    }

    // Lager et endepunkt for å sende en klage til kabal så vi lettere kan teste.
    // Kan fjernes når vi har fått på plass backend-logikken for å sende klagen til kabal når bruker ikke får medhold og saken går til KA.
    @PostMapping("/klagebehandling/send-klage-til-kabal")
    fun sendKlageTilKabal(@Valid @RequestBody hentKlageRequest: HentKlageRequest) {
        validerMiljo()
        validerTilganger(
            tilgangType = TilgangType.SKRIVE,
            authService,
            hentAktorId(hentKlageRequest.vedtakId, vedtakRepository)
        )
        return klageService.sendKlageTilKabal(hentKlageRequest)
    }

    object Mapper {
        fun tilKlageInitiellData(opprettKlageRequest: OpprettKlageRequest): KlageInitiellData {
            return KlageInitiellData(
                vedtakId = opprettKlageRequest.vedtakId,
                veilederIdent = opprettKlageRequest.veilederIdent,
                norskIdent = opprettKlageRequest.fnr.get(),
                klageDato = opprettKlageRequest.klagedato,
                klageJournalpostid = opprettKlageRequest.klageJournalpostid,
            )
        }

        fun tilKlageFormkravData(oppdaterFormkravRequest: OppdaterFormkravRequest): KlageFormkravData {
            return KlageFormkravData(
                formkravSignert = oppdaterFormkravRequest.signert,
                formkravPart = oppdaterFormkravRequest.part,
                formkravKonkret = oppdaterFormkravRequest.konkret,
                formkravKlagefristOpprettholdt = oppdaterFormkravRequest.klagefristOpprettholdt,
                formkravKlagefristUnntak = oppdaterFormkravRequest.klagefristUnntak,
                formkravBegrunnelseIntern = oppdaterFormkravRequest.formkravBegrunnelseIntern,
                formkravBegrunnelseBrev = oppdaterFormkravRequest.formkravBegrunnelseBrev
            )
        }
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
            when (eksternBrukerId) {
                is Fnr -> authService.sjekkTilgangTilBrukerOgEnhet(tilgangType, fnr = eksternBrukerId)
                is AktorId -> authService.sjekkTilgangTilBrukerOgEnhet(tilgangType, aktorId = eksternBrukerId)
                else -> throw ResponseStatusException(HttpStatus.FORBIDDEN)
            }
        }

        internal fun hentAktorId(vedtakId: Long, vedtakRepository: VedtaksstotteRepository): AktorId {
            return requireNotNull(vedtakRepository.hentVedtak(vedtakId)) {
                "Fant ingen vedtak med id $vedtakId."
            }.aktorId.let { AktorId.of(it) }
        }
    }
}

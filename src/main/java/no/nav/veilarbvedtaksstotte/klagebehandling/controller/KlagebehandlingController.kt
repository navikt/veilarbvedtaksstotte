package no.nav.veilarbvedtaksstotte.klagebehandling.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EksternBrukerId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils.isDevelopment
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.KlageController.Mapper.tilKlageAvvisningData
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.KlageController.Mapper.tilKlageFormkravData
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.KlageController.Mapper.tilKlageInitiellData
import no.nav.veilarbvedtaksstotte.klagebehandling.controller.KlageController.Mapper.tilProblemDetailResponse
import no.nav.veilarbvedtaksstotte.klagebehandling.domene.*
import no.nav.veilarbvedtaksstotte.klagebehandling.service.Feil
import no.nav.veilarbvedtaksstotte.klagebehandling.service.KlageService
import no.nav.veilarbvedtaksstotte.klagebehandling.service.Ok
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException


@RestController
@RequestMapping("/api/klagebehandling")
@Tag(
    name = "Klage på § 14 a-vedtak",
    description = "Funksjonalitet knyttet til klage på § 14 a-vedtak."
)
class KlageController(
    val klageService: KlageService,
    val authService: AuthService,
    val vedtakRepository: VedtaksstotteRepository
) {

    @PostMapping("/opprett-klage")
    @Operation(
        summary = "Opprett ny klagebehandling",
        description = "Starter en ny klagebehandling for et vedtak. Returnerer klagebehandlingId ved suksess."
    )
    @ApiResponsesStartKlagebehandlingSuksess
    @ApiResponsesKlagebehandlingFeil
    fun opprettKlagePa14aVedtak(@Valid @RequestBody opprettKlageRequest: OpprettKlageRequest): ResponseEntity<*> {
        validerMiljo()
        validerTilganger(TilgangType.SKRIVE, authService, opprettKlageRequest.fnr)

        return when (val resultat = klageService.startNyKlagebehandling(tilKlageInitiellData(opprettKlageRequest))) {
            is Feil -> tilProblemDetailResponse(resultat.årsak)
            is Ok -> ResponseEntity.ok(OpprettKlagebehandlingResponse(resultat.data))
        }
    }

    @PostMapping("/formkrav")
    @Operation(
        summary = "Oppdater formkrav",
        description = "Oppdaterer formkrav- og begrunnelsesdata for en klagebehandling."
    )
    @ApiResponsesOppdaterFormkravSuksess
    @ApiResponsesKlagebehandlingFeil
    fun oppdaterFormkrav(@Valid @RequestBody formkravrequest: OppdaterFormkravRequest): ResponseEntity<*> {
        validerMiljo()
        validerTilganger(
            TilgangType.SKRIVE,
            authService,
            hentAktorId(formkravrequest.vedtakId, vedtakRepository)
        )
        return when (val resultat =
            klageService.oppdaterFormkrav(formkravrequest.vedtakId, tilKlageFormkravData(formkravrequest))) {
            is Feil -> tilProblemDetailResponse(resultat.årsak)
            is Ok -> ResponseEntity.noContent().build<Unit>()
        }
    }

    @PostMapping("/avvis")
    @Operation(
        summary = "Avvis klage",
        description = "Avviser en klage basert på formkrav og begrunnelse."
    )
    @ApiResponsesAvvisKlageSuksess
    @ApiResponsesKlagebehandlingFeil
    fun avvisKlage(@RequestBody avvisKlageRequest: AvvisKlageRequest): ResponseEntity<*> {
        validerMiljo()
        validerTilganger(
            TilgangType.SKRIVE,
            authService,
            hentAktorId(avvisKlageRequest.vedtakId, vedtakRepository)
        )

        return when (val resultat =
            klageService.avvisKlage(avvisKlageRequest.vedtakId, tilKlageFormkravData(avvisKlageRequest))
        ) {
            is Feil -> tilProblemDetailResponse(resultat.årsak)
            is Ok -> ResponseEntity.noContent().build<Unit>()
        }
    }

    @PostMapping("/fullfor-avvisning")
    @Operation(
        summary = "Fullfør avvisning",
        description = "Fullfører en avvisning ved å registrere avvisningsbrev og ferdigstille klagebehandlingen."
    )
    @ApiResponsesFullførKlageavvisningSuksess
    @ApiResponsesKlagebehandlingFeil
    fun fullførAvvisning(@RequestBody fullførKlageAvvisningRequest: FullførKlageAvvisningRequest): ResponseEntity<*> {
        validerMiljo()
        validerTilganger(
            TilgangType.SKRIVE,
            authService,
            hentAktorId(fullførKlageAvvisningRequest.vedtakId, vedtakRepository)
        )

        return when (val resultat =
            klageService.fullførAvvisning(
                fullførKlageAvvisningRequest.vedtakId,
                tilKlageAvvisningData(`fullførKlageAvvisningRequest`)
            )) {
            is Feil -> tilProblemDetailResponse(resultat.årsak)
            is Ok -> ResponseEntity.noContent().build<Unit>()
        }
    }

    @PostMapping("/hent-klage")
    @Operation(
        summary = "Hent klagebehandling",
        description = "Henter klagebehandling for et vedtakId."
    )
    @ApiResponsesHentKlagebehandlingSuksess
    @ApiResponsesKlagebehandlingFeil
    fun hentKlage(@Valid @RequestBody hentKlageRequest: HentKlageRequest): ResponseEntity<*> {
        validerMiljo()
        validerTilganger(
            TilgangType.SKRIVE,
            authService,
            hentAktorId(hentKlageRequest.vedtakId, vedtakRepository)
        )
        return when (val resultat = klageService.hentKlage(hentKlageRequest.vedtakId)) {
            is Feil -> tilProblemDetailResponse(resultat.årsak)
            is Ok -> ResponseEntity.ok(HentKlagebehandlingResponse(resultat.data))
        }
    }

    // Lager et endepunkt for å sende en klage til kabal så vi lettere kan teste.
    // Kan fjernes når vi har fått på plass backend-logikken for å sende klagen til kabal når bruker ikke får medhold og saken går til KA.
    @PostMapping("/klagebehandling/send-klage-til-kabal")
    fun sendKlageTilKabal(@Valid @RequestBody hentKlageRequest: HentKlageRequest): ResponseEntity<*> {
        validerMiljo()
        validerTilganger(
            tilgangType = TilgangType.SKRIVE,
            authService,
            hentAktorId(hentKlageRequest.vedtakId, vedtakRepository)
        )

        return when (val resultat = klageService.sendKlageTilKabal(hentKlageRequest)) {
            is Feil -> tilProblemDetailResponse(resultat.årsak)
            is Ok -> ResponseEntity.noContent().build<Unit>()
        }
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

        fun tilKlageFormkravData(avvisKlageRequest: AvvisKlageRequest): KlageFormkravData {
            return KlageFormkravData(
                formkravSignert = avvisKlageRequest.signert,
                formkravPart = avvisKlageRequest.part,
                formkravKonkret = avvisKlageRequest.konkret,
                formkravKlagefristOpprettholdt = avvisKlageRequest.klagefristOpprettholdt,
                formkravKlagefristUnntak = avvisKlageRequest.klagefristUnntak,
                formkravBegrunnelseIntern = avvisKlageRequest.formkravBegrunnelseIntern,
                formkravBegrunnelseBrev = avvisKlageRequest.formkravBegrunnelseBrev
            )
        }

        fun tilKlageAvvisningData(fullførKlageAvvisning: FullførKlageAvvisningRequest): KlageAvvisningData {
            return KlageAvvisningData(
                fullførKlageAvvisning.avvisningsbrevJournalpostId
            )
        }

        fun tilProblemDetailResponse(årsak: Feil.Årsak): ResponseEntity<ProblemDetail> {
            return when (årsak) {
                Feil.Årsak.ULOVLIG_NÅVÆRENDE_KLAGEBEHANDLING_TILSTAND ->
                    tilProblemDetailResponse(
                        HttpStatus.CONFLICT,
                        "Ulovlig tilstand",
                        "Klagebehandlingen er i en tilstand som ikke tillater denne operasjonen."
                    )

                Feil.Årsak.PÅKLAGET_VEDTAK_IKKE_FUNNET ->
                    tilProblemDetailResponse(
                        HttpStatus.NOT_FOUND,
                        "Vedtak ikke funnet",
                        "Fant ikke vedtaket det klages på."
                    )

                Feil.Årsak.PÅKLAGET_VEDTAK_TILHØRER_IKKE_BRUKER ->
                    tilProblemDetailResponse(
                        HttpStatus.FORBIDDEN,
                        "Ingen tilgang",
                        "Vedtaket det klages på tilhører ikke innlogget bruker."
                    )

                Feil.Årsak.KLAGEDATO_ER_FREM_I_TID ->
                    tilProblemDetailResponse(
                        HttpStatus.BAD_REQUEST,
                        "Ugyldig klagedato",
                        "Klagedato kan ikke være frem i tid."
                    )

                Feil.Årsak.KLAGEDATO_ER_FØR_VEDTAK_FATTET_DATO ->
                    tilProblemDetailResponse(
                        HttpStatus.BAD_REQUEST,
                        "Ugyldig klagedato",
                        "Klagedato kan ikke være før datoen vedtaket ble fattet."
                    )

                Feil.Årsak.FORMKRAV_BEGRUNNELSE_SATT_UTEN_RIKTIGE_KRITERIER ->
                    tilProblemDetailResponse(
                        HttpStatus.BAD_REQUEST,
                        "Ugyldig begrunnelse",
                        "Begrunnelse er satt uten at riktige kriterier er oppfylt."
                    )

                Feil.Årsak.FORMKRAV_BEGRUNNELSE_MANGLER ->
                    tilProblemDetailResponse(
                        HttpStatus.BAD_REQUEST,
                        "Begrunnelse mangler",
                        "Begrunnelse må være satt for å oppfylle formkravene."
                    )

                Feil.Årsak.ALLE_FORMKRAV_MÅ_VÆRE_SATT ->
                    tilProblemDetailResponse(
                        HttpStatus.BAD_REQUEST,
                        "Formkrav mangler",
                        "Alle formkrav må være satt før klagen kan behandles."
                    )

                Feil.Årsak.FRIST_IKKE_OPPRETTHOLDT_KREVER_UNNTAK_SATT ->
                    tilProblemDetailResponse(
                        HttpStatus.BAD_REQUEST,
                        "Klagefrist ikke opprettholdt",
                        "Når klagefristen ikke er opprettholdt må unntak være satt."
                    )

                Feil.Årsak.KAN_IKKE_AVVISE_KLAGE_NÅR_FORMKRAV_OPPFYLT ->
                    tilProblemDetailResponse(
                        HttpStatus.CONFLICT,
                        "Kan ikke avvise klage",
                        "Klagen kan ikke avvises når alle formkrav er oppfylt."
                    )

                Feil.Årsak.AVVISNINGSBREV_JOURNALPOST_IKKE_FUNNET ->
                    tilProblemDetailResponse(
                        HttpStatus.NOT_FOUND,
                        "Journalpost ikke funnet",
                        "Fant ikke journalposten for avvisningsbrevet."
                    )

                Feil.Årsak.KLAGEBREV_JOURNALPOST_IKKE_FUNNET -> tilProblemDetailResponse(
                    HttpStatus.NOT_FOUND,
                    "Journalpost ikke funnet",
                    "Fant ikke journalposten for klagebrevet."
                )

                Feil.Årsak.KLAGEBREV_JOURNALPOST_TILHØRER_IKKE_BRUKER -> tilProblemDetailResponse(
                    HttpStatus.CONFLICT,
                    "Journalpost gjelder feil person",
                    "Journalposten gjelder annen personbruker."
                )

                Feil.Årsak.UKJENT_FEIL -> tilProblemDetailResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ukjent feil",
                    "Det oppstod en ukjent feil."
                )

                Feil.Årsak.KLAGE_IKKE_FUNNET -> tilProblemDetailResponse(
                    HttpStatus.NOT_FOUND,
                    "Klage ikke funnet",
                    "Fant ikke klagen det refereres til."
                )
            }.let { ResponseEntity.status(it.status).body(it) }
        }

        fun tilProblemDetailResponse(status: HttpStatus, tittel: String, beskrivelse: String): ProblemDetail {
            return ProblemDetail.forStatusAndDetail(status, beskrivelse).apply {
                this.title = tittel
            }
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

data class OpprettKlagebehandlingResponse(
    val klagebehandlingId: KlagebehandlingId
)

data class HentKlagebehandlingResponse(
    val klagebehandling: Klagebehandling
)
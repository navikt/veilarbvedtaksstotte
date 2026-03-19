package no.nav.veilarbvedtaksstotte.klagebehandling.controller

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.ProblemDetail

private const val PROBLEM_JSON = "application/problem+json"

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "200",
            description = "Klagebehandling opprettet",
            content = [Content(schema = Schema(implementation = OpprettKlagebehandlingResponse::class))]
        )
    ]
)
annotation class ApiResponsesStartKlagebehandlingSuksess

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponses(
    value = [
        ApiResponse(responseCode = "204", description = "Oppdatering av formkrav utført")
    ]
)
annotation class ApiResponsesOppdaterFormkravSuksess

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponses(
    value = [
        ApiResponse(responseCode = "204", description = "Avvisning av klage utført")
    ]
)
annotation class ApiResponsesAvvisKlageSuksess

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponses(
    value = [
        ApiResponse(responseCode = "204", description = "Fullføring av klageavvisning utført")
    ]
)
annotation class ApiResponsesFullførKlageavvisningSuksess

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "200",
            description = "Klagebehandling funnet",
            content = [Content(schema = Schema(implementation = HentKlagebehandlingResponse::class))]
        )
    ]
)
annotation class ApiResponsesHentKlagebehandlingSuksess

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "400",
            description = "Validerings-/domene-feil",
            content = [
                Content(
                    mediaType = PROBLEM_JSON,
                    schema = Schema(implementation = ProblemDetail::class),
                    examples = [
                        ExampleObject(
                            name = "KLAGEDATO_ER_FREM_I_TID",
                            summary = "Ugyldig klagedato",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Ugyldig klagedato",
                              "status": 400,
                              "detail": "Klagedato kan ikke være frem i tid."
                            }
                            """
                        ),
                        ExampleObject(
                            name = "KLAGEDATO_ER_FØR_VEDTAK_FATTET_DATO",
                            summary = "Ugyldig klagedato",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Ugyldig klagedato",
                              "status": 400,
                              "detail": "Klagedato kan ikke være før datoen vedtaket ble fattet."
                            }
                            """
                        ),
                        ExampleObject(
                            name = "FORMKRAV_BEGRUNNELSE_SATT_UTEN_RIKTIGE_KRITERIER",
                            summary = "Ugyldig begrunnelse",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Ugyldig begrunnelse",
                              "status": 400,
                              "detail": "Begrunnelse er satt uten at riktige kriterier er oppfylt."
                            }
                            """
                        ),
                        ExampleObject(
                            name = "FORMKRAV_BEGRUNNELSE_MANGLER",
                            summary = "Begrunnelse mangler",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Begrunnelse mangler",
                              "status": 400,
                              "detail": "Begrunnelse må være satt for å oppfylle formkravene."
                            }
                            """
                        ),
                        ExampleObject(
                            name = "ALLE_FORMKRAV_MÅ_VÆRE_SATT",
                            summary = "Formkrav mangler",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Formkrav mangler",
                              "status": 400,
                              "detail": "Alle formkrav må være satt før klagen kan behandles."
                            }
                            """
                        ),
                        ExampleObject(
                            name = "FRIST_IKKE_OPPRETTHOLDT_KREVER_UNNTAK_SATT",
                            summary = "Klagefrist ikke opprettholdt",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Klagefrist ikke opprettholdt",
                              "status": 400,
                              "detail": "Når klagefristen ikke er opprettholdt må unntak være satt."
                            }
                            """
                        )
                    ]
                )
            ]
        ),
        ApiResponse(
            responseCode = "403",
            description = "Ingen tilgang",
            content = [
                Content(
                    mediaType = PROBLEM_JSON,
                    schema = Schema(implementation = ProblemDetail::class),
                    examples = [
                        ExampleObject(
                            name = "PÅKLAGET_VEDTAK_TILHØRER_IKKE_BRUKER",
                            summary = "Ingen tilgang",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Ingen tilgang",
                              "status": 403,
                              "detail": "Vedtaket det klages på tilhører ikke innlogget bruker."
                            }
                            """
                        )
                    ]
                )
            ]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Ressurs ikke funnet",
            content = [
                Content(
                    mediaType = PROBLEM_JSON,
                    schema = Schema(implementation = ProblemDetail::class),
                    examples = [
                        ExampleObject(
                            name = "PÅKLAGET_VEDTAK_IKKE_FUNNET",
                            summary = "Vedtak ikke funnet",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Vedtak ikke funnet",
                              "status": 404,
                              "detail": "Fant ikke vedtaket det klages på."
                            }
                            """
                        ),
                        ExampleObject(
                            name = "AVVISNINGSBREV_JOURNALPOST_IKKE_FUNNET",
                            summary = "Journalpost ikke funnet",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Journalpost ikke funnet",
                              "status": 404,
                              "detail": "Fant ikke journalposten for avvisningsbrevet."
                            }
                            """
                        ),
                        ExampleObject(
                            name = "KLAGEBREV_JOURNALPOST_IKKE_FUNNET",
                            summary = "Journalpost ikke funnet",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Journalpost ikke funnet",
                              "status": 404,
                              "detail": "Fant ikke journalposten for klagebrevet."
                            }
                            """
                        ),
                        ExampleObject(
                            name = "KLAGE_IKKE_FUNNET",
                            summary = "Klage ikke funnet",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Klage ikke funnet",
                              "status": 404,
                              "detail": "Fant ikke klagen det refereres til."
                            }
                            """
                        )
                    ]
                )
            ]
        ),
        ApiResponse(
            responseCode = "409",
            description = "Konflikt / ulovlig tilstand",
            content = [
                Content(
                    mediaType = PROBLEM_JSON,
                    schema = Schema(implementation = ProblemDetail::class),
                    examples = [
                        ExampleObject(
                            name = "ULOVLIG_NÅVÆRENDE_KLAGEBEHANDLING_TILSTAND",
                            summary = "Ulovlig tilstand",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Ulovlig tilstand",
                              "status": 409,
                              "detail": "Klagebehandlingen er i en tilstand som ikke tillater denne operasjonen."
                            }
                            """
                        ),
                        ExampleObject(
                            name = "KAN_IKKE_AVVISE_KLAGE_NÅR_FORMKRAV_OPPFYLT",
                            summary = "Kan ikke avvise klage",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Kan ikke avvise klage",
                              "status": 409,
                              "detail": "Klagen kan ikke avvises når alle formkrav er oppfylt."
                            }
                            """
                        ),
                        ExampleObject(
                            name = "KLAGEBREV_JOURNALPOST_TILHØRER_IKKE_BRUKER",
                            summary = "Journalpost gjelder feil person",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Journalpost gjelder feil person",
                              "status": 409,
                              "detail": "Journalposten gjelder annen personbruker."
                            }
                            """
                        )
                    ]
                )
            ]
        ),
        ApiResponse(
            responseCode = "500",
            description = "Ukjent feil",
            content = [
                Content(
                    mediaType = PROBLEM_JSON,
                    schema = Schema(implementation = ProblemDetail::class),
                    examples = [
                        ExampleObject(
                            name = "UKJENT_FEIL",
                            summary = "Ukjent feil",
                            value = """
                            {
                              "type": "about:blank",
                              "title": "Ukjent feil",
                              "status": 500,
                              "detail": "Det oppstod en ukjent feil."
                            }
                            """
                        )
                    ]
                )
            ]
        )
    ]
)
annotation class ApiResponsesKlagebehandlingFeil
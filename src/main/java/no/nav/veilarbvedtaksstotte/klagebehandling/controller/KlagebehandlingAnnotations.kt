package no.nav.veilarbvedtaksstotte.klagebehandling.controller

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import java.net.URI

@Schema(description = "Problem Detail for klagebehandling")
data class KlagebehandlingProblemDetailResponse(
    val type: URI,
    val title: KlagebehandlingProblemDetailÅrsak,
    val status: Int,
    val detail: String,
) {
    enum class KlagebehandlingProblemDetailÅrsak {
        // Bad Request
        KLAGEDATO_ER_FREM_I_TID,
        KLAGEDATO_ER_FØR_VEDTAK_FATTET_DATO,
        FORMKRAV_BEGRUNNELSE_SATT_UTEN_RIKTIGE_KRITERIER,
        FORMKRAV_BEGRUNNELSE_MANGLER,
        ALLE_FORMKRAV_MÅ_VÆRE_SATT,
        FRIST_IKKE_OPPRETTHOLDT_KREVER_UNNTAK_SATT,

        // Conflict
        ULOVLIG_NÅVÆRENDE_KLAGEBEHANDLING_TILSTAND,
        KAN_IKKE_AVVISE_KLAGE_NÅR_FORMKRAV_OPPFYLT,
        KLAGEBREV_JOURNALPOST_TILHØRER_IKKE_BRUKER,

        // Forbidden
        PÅKLAGET_VEDTAK_TILHØRER_IKKE_BRUKER,

        // Not Found
        AVVISNINGSBREV_JOURNALPOST_IKKE_FUNNET,
        KLAGEBREV_JOURNALPOST_IKKE_FUNNET,
        KLAGE_IKKE_FUNNET,
        PÅKLAGET_VEDTAK_IKKE_FUNNET,

        // Internal Server Error
        UKJENT_FEIL
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = [
                Content(
                    mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                    schema = Schema(implementation = KlagebehandlingProblemDetailResponse::class),
                )
            ]
        ),
        ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content = [
                Content(
                    mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                    schema = Schema(implementation = KlagebehandlingProblemDetailResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = [
                Content(
                    mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                    schema = Schema(implementation = KlagebehandlingProblemDetailResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "409",
            description = "Conflict",
            content = [
                Content(
                    mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                    schema = Schema(implementation = KlagebehandlingProblemDetailResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = [
                Content(
                    mediaType = APPLICATION_PROBLEM_JSON_VALUE,
                    schema = Schema(implementation = KlagebehandlingProblemDetailResponse::class)
                )
            ]
        )
    ]
)
annotation class KlagebehandlingFeilApiResponse
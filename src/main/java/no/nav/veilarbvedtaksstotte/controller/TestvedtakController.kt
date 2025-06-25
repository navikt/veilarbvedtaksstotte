package no.nav.veilarbvedtaksstotte.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarbvedtaksstotte.controller.dto.TestvedtakRequest
import no.nav.veilarbvedtaksstotte.controller.dto.toVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.TestvedtakService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1/test/vedtak")
@Tag(
    name = "Fattede § 14 a-vedtak for testmiljø",
    description = "Funksjonalitet knyttet til å lagre, hente og slette § 14 a-vedtak i testmiljø."
)
class TestvedtakController(
    private val testvedtakService: TestvedtakService,
    private val authService: AuthService,
    private val aktorOppslagClient: AktorOppslagClient
) {

    @PostMapping
    @Operation(
        summary = "Fatt § 14 a-vedtak",
        description = "Fatt § 14 a-vedtak på person. Gjelder kun i preprodmiljøer og er ikke tiltenk bruk for produksjon",
        responses = [
            ApiResponse(
                responseCode = "200",
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun fattTestVedtak(@RequestBody testvedtakRequest: TestvedtakRequest) {
        if (!authService.harSystemTilSystemTilgangMedEkstraRolle("fatt-14a-vedtak")) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        if (EnvironmentUtils.isDevelopment().orElse(false)) {
            val aktorId: AktorId = aktorOppslagClient.hentAktorId(testvedtakRequest.fnr)
            testvedtakService.lagreTestvedtak(testvedtakRequest.toVedtak(aktorId))
            return
        }
        throw UnsupportedOperationException("Fatt vedtak er ikke støttet i produksjon. Denne funksjonaliteten er kun tilgjengelig i preprod-miljøer for testing av ny vedtaksløsning." +
                "Hvis du ønsker å teste dette, må du kjøre applikasjonen i preprod-miljøet.")
    }

    @PostMapping("/hent-vedtak") //skal vi bare la de bruke gjeldenede 14a-endepunktet?
    @Operation(
        summary = "Hent § 14 a-vedtak på person",
        description = "Hent § 14 a-vedtak på person. Gjelder kun i preprodmiljøer og er ikke tiltenk bruk for produksjon",
        responses = [
            ApiResponse(
                responseCode = "200",
                content = [Content(array = ArraySchema(schema = Schema(implementation = Vedtak::class)))]
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun hentTestVedtak(@RequestBody fnr: Fnr): Vedtak? {
        if (!authService.harSystemTilSystemTilgangMedEkstraRolle("fatt-14a-vedtak")) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        if (EnvironmentUtils.isDevelopment().orElse(false)) {
            val aktorId: AktorId = aktorOppslagClient.hentAktorId(fnr)
            return testvedtakService.hentTestvedtak(aktorId)
        }
        throw UnsupportedOperationException("Henting av vedtak er ikke støttet i produksjon. Denne funksjonaliteten er kun tilgjengelig i preprod-miljøer for testing av ny vedtaksløsning.")
    }

    @DeleteMapping
    @Operation(
        summary = "Slett § 14 a-vedtak på person",
        description = "Slett alle § 14 a-vedtak på person. Gjelder kun i preprodmiljøer og er ikke tiltenk bruk for produksjon",
        responses = [
            ApiResponse(
                responseCode = "200",
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun slettTestVedtak(@RequestBody fnr: Fnr) {
        if (!authService.harSystemTilSystemTilgangMedEkstraRolle("fatt-14a-vedtak")) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        if (EnvironmentUtils.isDevelopment().orElse(false)) {
            val aktorId: AktorId = aktorOppslagClient.hentAktorId(fnr)
            testvedtakService.slettTestvedtak(aktorId)
            return
        }
        throw UnsupportedOperationException("Sletting av vedtak er ikke støttet i produksjon. Denne funksjonaliteten er kun tilgjengelig i preprod-miljøer for testing av ny vedtaksløsning.")
    }
}
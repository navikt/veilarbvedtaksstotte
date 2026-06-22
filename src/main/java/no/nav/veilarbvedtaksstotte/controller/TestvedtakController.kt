package no.nav.veilarbvedtaksstotte.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarbvedtaksstotte.controller.dto.OpprettTestvedtakRequest
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
        description = "Fatt § 14 a-vedtak på testperson. Gjelder kun i testmiljøet og er ikke tiltenkt bruk i produksjon",
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
    fun fattTestVedtak(
        @RequestBody @Valid opprettTestvedtakRequest: OpprettTestvedtakRequest
    ) {
        if (!authService.harSystemTilSystemTilgangMedEkstraRolle("testdata-14a-vedtak")) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        if (EnvironmentUtils.isDevelopment().orElse(false)) {
            val aktorId: AktorId = aktorOppslagClient.hentAktorId(opprettTestvedtakRequest.fnr)
            testvedtakService.lagreTestvedtak(
                opprettTestvedtakRequest.toVedtak(aktorId),
                opprettTestvedtakRequest.fnr
            )
            return
        }
        throw ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Fatt vedtak er ikke støttet i produksjon. Denne funksjonaliteten er kun tilgjengelig i testmiljøet for testing av ny vedtaksløsning."
        )
    }

    @PostMapping("/hent-vedtak")
    @Operation(
        summary = "Hent alle § 14 a-vedtak",
        description = "Hent alle § 14 a-vedtak på testperson. Gjelder kun i testmiljøet og er ikke tiltenkt bruk i produksjon",
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
    fun hentTestVedtak(@RequestBody @Valid testvedtakRequest: TestvedtakRequest): List<Vedtak> {
        if (!authService.harSystemTilSystemTilgangMedEkstraRolle("testdata-14a-vedtak")) { 
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        if (EnvironmentUtils.isDevelopment().orElse(false)) {
            val aktorId: AktorId = aktorOppslagClient.hentAktorId(testvedtakRequest.fnr)
            return testvedtakService.hentAlleTestvedtak(aktorId)
        }
        throw ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Henting av testvedtak er ikke støttet i produksjon. Denne funksjonaliteten er kun tilgjengelig i testmiljøet for testing av ny vedtaksløsning."
        )
    }

    @DeleteMapping
    @Operation(
        summary = "Slett § 14 a-vedtak på person",
        description = "Slett alle § 14 a-vedtak på testperson. Gjelder kun i testmiljøet og er ikke tiltenkt bruk i produksjon",
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
    fun slettTestVedtak(@RequestBody @Valid testvedtakRequest: TestvedtakRequest) {
        if (!authService.harSystemTilSystemTilgangMedEkstraRolle("testdata-14a-vedtak")) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        if (EnvironmentUtils.isDevelopment().orElse(false)) {
            val aktorId: AktorId = aktorOppslagClient.hentAktorId(testvedtakRequest.fnr)
            testvedtakService.slettGjeldendeTestvedtak(aktorId)
            return
        }
        throw ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Sletting av testvedtak er ikke støttet i produksjon. Denne funksjonaliteten er kun tilgjengelig i testmiljøet for testing av ny vedtaksløsning."
        )
    }
}

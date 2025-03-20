package no.nav.veilarbvedtaksstotte.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.common.audit_log.cef.AuthorizationDecision
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.common.audit_log.log.AuditLogger
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarbvedtaksstotte.annotations.EksterntEndepunkt
import no.nav.veilarbvedtaksstotte.controller.dto.Gjeldende14aVedtakDto
import no.nav.veilarbvedtaksstotte.controller.dto.toGjeldende14aVedtakDto
import no.nav.veilarbvedtaksstotte.controller.v2.dto.Gjeldende14aVedtakRequest
import no.nav.veilarbvedtaksstotte.service.AuthService
import no.nav.veilarbvedtaksstotte.service.Gjeldende14aVedtakService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException


@RestController
@RequestMapping("/api")
@Tag(
    name = "Gjeldende § 14 a-vedtak",
    description = "Funksjonalitet knyttet til gjeldende § 14 a-vedtak."
)
class Gjeldende14aVedtakController(
    val authService: AuthService,
    val gjeldende14aVedtakService: Gjeldende14aVedtakService,
    val auditLogger: AuditLogger
) {

    @EksterntEndepunkt
    @PostMapping("/ekstern/hent-gjeldende-14a-vedtak")
    @Operation(
        summary = "Henter personens gjeldende § 14 a-vedtak",
        description = "Henter det gjeldende § 14 a-vedtaket for den spesifiserte brukeren. Har ikke tilgangssjekk på fagsystemroller, så dette må gjøres av konsumentene.",
        responses = [
            ApiResponse(
                responseCode = "200",
                content = [Content(schema = Schema(implementation = Gjeldende14aVedtakDto::class))]
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun hentGjeldende14aVedtakEksternt(@RequestBody gjeldende14aVedtakRequest: Gjeldende14aVedtakRequest): Gjeldende14aVedtakDto? {
        sjekkLesetilgang(
            fnr = gjeldende14aVedtakRequest.fnr,
            veilederTilgangssjekk = ::sjekkVeilederUtenModiarolleTilgangTilBruker)
        return gjeldende14aVedtakService.hentGjeldende14aVedtak(gjeldende14aVedtakRequest.fnr)?.toGjeldende14aVedtakDto()
    }

    @PostMapping("/hent-gjeldende-14a-vedtak")
    @Operation(
        summary = "Hent siste 14a vedtak",
        description = "Henter det siste registrerte § 14 a-vedtaket for den spesifiserte brukeren.",
        responses = [
            ApiResponse(
                responseCode = "200",
                content = [Content(schema = Schema(implementation = Gjeldende14aVedtakDto::class))]
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun hentGjeldende14aVedtak(@RequestBody gjeldende14aVedtakRequest: Gjeldende14aVedtakRequest): Gjeldende14aVedtakDto? {
        sjekkLesetilgang(
            fnr = gjeldende14aVedtakRequest.fnr,
            veilederTilgangssjekk = ::sjekkVeilederTilgangTilBruker
        )

        val gjeldende14aVedtakDto = gjeldende14aVedtakService.hentGjeldende14aVedtak(gjeldende14aVedtakRequest.fnr)?.toGjeldende14aVedtakDto()
        gjeldende14aVedtakDto?.let { auditLogger.log(
            CefMessage.builder()
                .timeEnded(System.currentTimeMillis())
                .applicationName("veilarbvedtaksstotte")
                .sourceUserId(authService.innloggetVeilederIdent)
                .authorizationDecision(AuthorizationDecision.PERMIT)
                .event(CefMessageEvent.ACCESS)
                .severity(CefMessageSeverity.INFO)
                .name("veilarbvedtaksstotte-audit-log")
                .destinationUserId(gjeldende14aVedtakRequest.fnr.get())
                .extension("msg", "Veileder har hentet person sitt gjeldende § 14 a-vedtak")
                .build()
        ) }
        return gjeldende14aVedtakDto
    }

    private fun sjekkLesetilgang(fnr: Fnr, veilederTilgangssjekk: (fnr: Fnr) -> Unit) {
        if (authService.erSystemBruker()) {
            if (!authService.harSystemTilSystemTilgangMedEkstraRolle("gjeldende-14a-vedtak")) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN)
            }
        } else if (authService.erEksternBruker()) {
            authService.sjekkEksternbrukerTilgangTilBruker(fnr)
        } else {
            veilederTilgangssjekk(fnr)
        }
    }

    private fun sjekkVeilederUtenModiarolleTilgangTilBruker(fnr: Fnr) {
        authService.sjekkVeilederUtenModiarolleTilgangTilBruker(fnr)
    }

    private fun sjekkVeilederTilgangTilBruker(fnr: Fnr) {
        authService.sjekkVeilederTilgangTilBruker(tilgangType = TilgangType.LESE, fnr = fnr)
    }
}

package no.nav.veilarbvedtaksstotte.controller.v2

import no.nav.veilarbvedtaksstotte.controller.v2.dto.UtkastRequest
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.service.VedtakService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RestController
@RequestMapping("/api/v2")
class UtkastV2Controller(
    val vedtakService: VedtakService
) {
    @PostMapping("/hent-utkast")
    fun hentUtkast(@RequestBody utkastRequest: UtkastRequest): Vedtak {
        return vedtakService.hentUtkast(utkastRequest.fnr)
    }
    @PostMapping("/utkast/hent-harUtkast")
    fun harUtkast(@RequestBody utkastRequest: UtkastRequest): Boolean {
        return vedtakService.harUtkast(utkastRequest.fnr)
    }
}

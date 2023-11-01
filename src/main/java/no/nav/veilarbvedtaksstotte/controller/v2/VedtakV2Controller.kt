package no.nav.veilarbvedtaksstotte.controller.v2

import no.nav.veilarbvedtaksstotte.controller.v2.dto.VedtakRequest
import no.nav.veilarbvedtaksstotte.domain.arkiv.ArkivertVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.service.ArenaVedtakService
import no.nav.veilarbvedtaksstotte.service.VedtakService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestBody

@RestController
@RequestMapping("/api/v2/vedtak")
class VedtakV2Controller(
    val vedtakService: VedtakService,
    val arenaVedtakService: ArenaVedtakService,
) {
    @PostMapping("/hent-fattet")
    fun hentFattedeVedtak(@RequestBody vedtakRequest: VedtakRequest): List<Vedtak> {
        return vedtakService.hentFattedeVedtak(vedtakRequest.fnr)
    }

    @PostMapping("/hent-arena")
    fun hentVedtakFraArena(@RequestBody vedtakRequest: VedtakRequest): List<ArkivertVedtak> {
        return arenaVedtakService.hentVedtakFraArena(vedtakRequest.fnr)
    }

}


package no.nav.veilarbvedtaksstotte.controller.v2

import no.nav.veilarbvedtaksstotte.controller.dto.PersonRequestDTO
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
    @PostMapping("/fattet")
    fun hentFattedeVedtak(@RequestBody personRequestDTO: PersonRequestDTO): List<Vedtak> {
        return vedtakService.hentFattedeVedtak(personRequestDTO.fnr)
    }

    @PostMapping("/arena")
    fun hentVedtakFraArena(@RequestBody personRequestDTO: PersonRequestDTO): List<ArkivertVedtak> {
        return arenaVedtakService.hentVedtakFraArena(personRequestDTO.fnr)
    }

}


package no.nav.veilarbvedtaksstotte.controller.v2

import no.nav.veilarbvedtaksstotte.controller.dto.PersonRequestDTO
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.service.VedtakService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RestController
@RequestMapping("/api/v2/utkast")
class UtkastV2Controller(
    val vedtakService: VedtakService
) {
    @PostMapping
    fun hentUtkast(@RequestBody personRequestDTO: PersonRequestDTO): Vedtak {
        return vedtakService.hentUtkast(personRequestDTO.fnr)
    }
    @PostMapping("/harUtkast")
    fun harUtkast(@RequestBody personRequestDTO: PersonRequestDTO): Boolean {
        return vedtakService.harUtkast(personRequestDTO.fnr)
    }
}

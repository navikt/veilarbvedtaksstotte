package no.nav.veilarbvedtaksstotte.controller

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.controller.dto.InnsatsbehovDTO
import no.nav.veilarbvedtaksstotte.service.InnsatsbehovService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/innsatsbehov")
class InnsatsbehovController(val innsatsbehovService: InnsatsbehovService) {

    @GetMapping
    fun hentInnsatsbehov(@RequestParam("fnr") fnr: Fnr): ResponseEntity<InnsatsbehovDTO> {
        return innsatsbehovService.sisteInnsatsbehov(fnr)
            ?.let { ResponseEntity(InnsatsbehovDTO.fraInnsatsbehov(it), HttpStatus.OK) }
            ?: ResponseEntity(HttpStatus.NO_CONTENT)
    }
}

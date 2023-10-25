package no.nav.veilarbvedtaksstotte.controller.v2

import no.nav.veilarbvedtaksstotte.controller.v2.dto.UtrullingRequest
import no.nav.veilarbvedtaksstotte.service.UtrullingService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/utrulling")
class UtrullingV2Controller(
    val utrullingService: UtrullingService
) {
    @PostMapping("/tilhorerBrukerUtrulletKontor")
    fun tilhorerBrukerUtrulletKontor(@RequestBody utrullingRequest: UtrullingRequest): Boolean {
        return utrullingService.tilhorerBrukerUtrulletKontor(utrullingRequest.fnr)
    }
}

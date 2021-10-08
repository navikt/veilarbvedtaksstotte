package no.nav.veilarbvedtaksstotte.controller;

import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalDetaljert;
import no.nav.veilarbvedtaksstotte.domain.vedtak.InnsatsgruppeDetaljert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/open/api/kodeverk")
public class KodeverkController {

    @GetMapping("/innsatsgruppe")
    public InnsatsgruppeDetaljert[] getInnsatsgrupper() {
        return InnsatsgruppeDetaljert.values();
    }

    @GetMapping("/hovedmal")
    public HovedmalDetaljert[] getHovedmal() {
        return HovedmalDetaljert.values();
    }
}

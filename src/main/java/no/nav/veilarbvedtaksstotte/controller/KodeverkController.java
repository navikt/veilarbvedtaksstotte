package no.nav.veilarbvedtaksstotte.controller;

import no.nav.veilarbvedtaksstotte.controller.dto.KodeverkDTO;
import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalDetaljert;
import no.nav.veilarbvedtaksstotte.domain.vedtak.InnsatsgruppeDetaljert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/open/api/kodeverk")
public class KodeverkController {
    KodeverkDTO kodeverk = new KodeverkDTO();

    @GetMapping("/innsatsgruppe")
    public InnsatsgruppeDetaljert[] getInnsatsgrupper() {
        return kodeverk.getInnsatsgrupper();
    }

    @GetMapping("/hovedmal")
    public HovedmalDetaljert[] getHovedmal() {
        return kodeverk.getHovedmal();
    }

    @GetMapping("/innsatsgruppeoghovedmal")
    public KodeverkDTO getKodeverk() {
        return kodeverk;
    }
}

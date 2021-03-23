package no.nav.veilarbvedtaksstotte.controller;

import no.nav.common.types.identer.EnhetId;
import no.nav.veilarbvedtaksstotte.controller.dto.HarTilgangDTO;
import no.nav.veilarbvedtaksstotte.service.TilgangskontrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tilgang")
public class TilgangskontrollController {

    private final TilgangskontrollService tilgangskontrollService;

    @Autowired
    public TilgangskontrollController(TilgangskontrollService tilgangskontrollService) {
        this.tilgangskontrollService = tilgangskontrollService;
    }

    @GetMapping("/{enhetId}")
    public HarTilgangDTO harTilgang(@PathVariable EnhetId enhetId) {
        return new HarTilgangDTO(tilgangskontrollService.harEnhetTilgang(enhetId));
    }

}

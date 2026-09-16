package no.nav.veilarbvedtaksstotte.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.veilarbvedtaksstotte.service.BeslutteroversiktService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beslutteroversikt")
@Tag(
        name = "Kvalitetssikringsoversikt",
        description = "Funksjonalitet knyttet til kvalitetssikringsoversikten. Kvalitetssikringsoversikten gir mulighet " +
                "for kvalitetssikrere å følge opp utkast til § 14 a-vedtak som krever kvalitetssikring."
)
public class BeslutteroversiktController {

    private final BeslutteroversiktService beslutteroversiktService;

    @Autowired
    public BeslutteroversiktController(BeslutteroversiktService beslutteroversiktService) {
        this.beslutteroversiktService = beslutteroversiktService;
    }
}

package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.domain.Veileder;
import no.nav.fo.veilarbvedtaksstotte.utils.AutentiseringUtils;
import org.springframework.stereotype.Component;

@Component
public class VeilederService {

    public Veileder hentVeilederFraToken() {

        String ident = AutentiseringUtils.hentIdent()
                .orElseThrow(() -> new RuntimeException("Fant ikke ident for veileder"));

        // TODO: Gjøre oppslag for å finne enhetId?
        return new Veileder()
                .setIdent(ident)
                .setEnhetId("0000");
    }

}

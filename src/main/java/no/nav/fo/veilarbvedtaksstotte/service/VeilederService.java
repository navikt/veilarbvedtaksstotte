package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.domain.Veileder;
import no.nav.fo.veilarbvedtaksstotte.utils.AutentiseringUtils;
import org.springframework.stereotype.Service;

@Service
public class VeilederService {

    public Veileder hentVeilederFraToken() {

        String ident = AutentiseringUtils.hentIdent()
                .orElseThrow(() -> new RuntimeException("Fant ikke ident for veileder"));

        return new Veileder().setIdent(ident);
    }

}
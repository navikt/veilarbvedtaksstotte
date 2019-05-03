package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.utils.AutentiseringUtils;
import org.springframework.stereotype.Service;

@Service
public class VeilederService {

    public String hentVeilederIdentFraToken() {
        return AutentiseringUtils.hentIdent()
                .orElseThrow(() -> new RuntimeException("Fant ikke ident for veileder"));

    }
}

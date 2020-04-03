package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.client.VeiledereOgEnhetClient;
import no.nav.veilarbvedtaksstotte.domain.Veileder;
import no.nav.veilarbvedtaksstotte.utils.AutentiseringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class VeilederService {

    private final VeiledereOgEnhetClient veiledereOgEnhetClient;

    @Inject
    public VeilederService(VeiledereOgEnhetClient veiledereOgEnhetClient) {
        this.veiledereOgEnhetClient = veiledereOgEnhetClient;
    }

    public String hentVeilederIdentFraToken() {
        return AutentiseringUtils.hentIdent()
                .orElseThrow(() -> new RuntimeException("Fant ikke ident for veileder"));
    }

    public Veileder hentVeileder(String veilederId) {
        return veiledereOgEnhetClient.hentVeileder(veilederId);
    }

    public String hentEnhetNavn(String enhetId) {
        return veiledereOgEnhetClient.hentEnhetNavn(enhetId);
    }

    public String hentInnloggetVeilederNavn() {
        Veileder veileder = veiledereOgEnhetClient.hentVeileder(hentVeilederIdentFraToken());
        return veileder != null ? veileder.getNavn() : null ;
    }
}

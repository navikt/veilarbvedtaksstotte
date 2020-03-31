package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.client.VeiledereOgEnhetClient;
import no.nav.veilarbvedtaksstotte.domain.Veileder;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class VeilederService {

    private final VeiledereOgEnhetClient veiledereOgEnhetClient;

    @Inject
    public VeilederService(VeiledereOgEnhetClient veiledereOgEnhetClient) {
        this.veiledereOgEnhetClient = veiledereOgEnhetClient;
    }

    public Veileder hentVeileder(String veilederId) {
        return veiledereOgEnhetClient.hentVeileder(veilederId);
    }

    public String hentEnhetNavn(String enhetId) {
        return veiledereOgEnhetClient.hentEnhetNavn(enhetId);
    }

}

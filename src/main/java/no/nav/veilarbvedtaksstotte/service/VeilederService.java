package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.client.api.veilederogenhet.VeiledereOgEnhetClient;
import no.nav.veilarbvedtaksstotte.client.api.veilederogenhet.Veileder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VeilederService {

    private final VeiledereOgEnhetClient veiledereOgEnhetClient;

    @Autowired
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

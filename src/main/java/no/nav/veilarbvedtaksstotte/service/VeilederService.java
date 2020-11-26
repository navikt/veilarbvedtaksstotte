package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeiledereOgEnhetClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.Veileder;
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

package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.Veileder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class VeilederService {

    private final VeilarbveilederClient veiledereOgEnhetClient;

    @Autowired
    public VeilederService(VeilarbveilederClient veiledereOgEnhetClient) {
        this.veiledereOgEnhetClient = veiledereOgEnhetClient;
    }

    public Veileder hentVeileder(String veilederId) {
        return veiledereOgEnhetClient.hentVeileder(veilederId);
    }

    public Optional<Veileder> hentVeilederEllerNull(String veilederId) {
        try {
            return Optional.ofNullable(veiledereOgEnhetClient.hentVeileder(veilederId));
        } catch (RuntimeException e) {
            log.warn("Feil ved henting av veileder", e);
            return Optional.empty();
        }
    }

    public String hentEnhetNavn(String enhetId) {
        return veiledereOgEnhetClient.hentEnhetNavn(enhetId);
    }

}

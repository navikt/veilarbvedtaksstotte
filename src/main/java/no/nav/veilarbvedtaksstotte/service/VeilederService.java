package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.Veileder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class VeilederService {

    private final VeilarbveilederClient veilarbveilederClient;

    @Autowired
    public VeilederService(VeilarbveilederClient veilarbveilederClient) {
        this.veilarbveilederClient = veilarbveilederClient;
    }

    public Veileder hentVeileder(String veilederId) {
        return veilarbveilederClient.hentVeileder(veilederId);
    }

    public Optional<Veileder> hentVeilederEllerNull(String veilederId) {
        try {
            return Optional.ofNullable(veilarbveilederClient.hentVeileder(veilederId));
        } catch (RuntimeException e) {
            log.warn("Feil ved henting av veileder", e);
            return Optional.empty();
        }
    }

    public String hentEnhetNavn(String enhetId) {
        return veilarbveilederClient.hentEnhetNavn(enhetId);
    }

}

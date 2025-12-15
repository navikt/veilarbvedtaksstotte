package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.arena.dto.VeilarbArenaOppfolging;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VeilarbarenaService {
    private final VeilarbarenaClient veilarbarenaClient;

    public VeilarbarenaService(VeilarbarenaClient veilarbarenaClient) {
        this.veilarbarenaClient = veilarbarenaClient;
    }

    public Optional<EnhetId> hentOppfolgingsenhet(Fnr fnr) {
        return veilarbarenaClient.hentOppfolgingsbruker(fnr)
                .map(VeilarbArenaOppfolging::getNavKontor)
                .map(EnhetId::of);
    }
}

package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbArenaOppfolging;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@Service
public class VeilarbarenaService {
    private final VeilarbarenaClient veilarbarenaClient;

    public VeilarbarenaService(VeilarbarenaClient veilarbarenaClient) {
        this.veilarbarenaClient = veilarbarenaClient;
    }

    public Optional<EnhetId> hentOppfolgingsenhet(Fnr fnr) {
        return ofNullable(veilarbarenaClient.hentOppfolgingsbruker(fnr))
                .map(VeilarbArenaOppfolging::getNavKontor)
                .map(EnhetId::of);
    }

    public Optional<String> hentFormidlingsgruppekode(Fnr fnr) {
        return ofNullable(veilarbarenaClient.hentOppfolgingsbruker(fnr))
                .map(VeilarbArenaOppfolging::getFormidlingsgruppekode);
    }

    public boolean erBrukerInaktivIArena(Fnr fnr) {
        return hentFormidlingsgruppekode(fnr)
                .map("ISERV"::equals)
                .orElse(false);
    }


}

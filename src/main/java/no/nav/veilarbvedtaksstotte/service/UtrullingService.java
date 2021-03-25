package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.repository.UtrullingRepository;
import no.nav.veilarbvedtaksstotte.repository.domain.UtrulletEnhet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtrullingService {

    private final UtrullingRepository utrullingRepository;

    private final VeilarbarenaClient veilarbarenaClient;

    @Autowired
    public UtrullingService(UtrullingRepository utrullingRepository, VeilarbarenaClient veilarbarenaClient) {
        this.utrullingRepository = utrullingRepository;
        this.veilarbarenaClient = veilarbarenaClient;
    }

    public List<UtrulletEnhet> hentAlleUtrullinger() {
        return utrullingRepository.hentAlleUtrullinger();
    }

    public void leggTilUtrulling(EnhetId enhetId) {
        utrullingRepository.leggTilUtrulling(enhetId);
    }

    public void fjernUtrulling(EnhetId enhetId) {
        utrullingRepository.fjernUtrulling(enhetId);
    }

    public boolean erUtrullet(EnhetId enhetId) {
        return utrullingRepository.erUtrullet(enhetId);
    }

    public boolean tilhorerBrukerUtrulletKontor(Fnr fnr) {
        EnhetId oppfolgingsenhet = veilarbarenaClient.oppfolgingsenhet(fnr);
        return utrullingRepository.erUtrullet(oppfolgingsenhet);
    }

}

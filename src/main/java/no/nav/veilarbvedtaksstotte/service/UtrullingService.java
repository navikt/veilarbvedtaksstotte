package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilederEnheterDTO;
import no.nav.veilarbvedtaksstotte.repository.UtrullingRepository;
import no.nav.veilarbvedtaksstotte.repository.domain.UtrulletEnhet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UtrullingService {

    private final UtrullingRepository utrullingRepository;

    private final VeilarbarenaClient veilarbarenaClient;

    private final VeilarbveilederClient veilarbveilederClient;

    @Autowired
    public UtrullingService(
            UtrullingRepository utrullingRepository,
            VeilarbarenaClient veilarbarenaClient,
            VeilarbveilederClient veilarbveilederClient
    ) {
        this.utrullingRepository = utrullingRepository;
        this.veilarbarenaClient = veilarbarenaClient;
        this.veilarbveilederClient = veilarbveilederClient;
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
        try {
            EnhetId oppfolgingsenhet = veilarbarenaClient.oppfolgingsenhet(fnr);
            return utrullingRepository.erUtrullet(oppfolgingsenhet);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean tilhorerInnloggetVeilederUtrulletKontor() {
        VeilederEnheterDTO veilederEnheterDTO = veilarbveilederClient.hentInnloggetVeilederEnheter();
        List<EnhetId> enhetIder = veilederEnheterDTO.getEnhetliste().stream()
                .map(portefoljeEnhet -> EnhetId.of(portefoljeEnhet.getEnhetId()))
                .collect(Collectors.toList());

        return utrullingRepository.erMinstEnEnhetUtrullet(enhetIder);
    }

}

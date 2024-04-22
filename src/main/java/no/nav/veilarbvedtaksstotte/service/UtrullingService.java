package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.arena.dto.VeilarbArenaOppfolging;
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.VeilederEnheterDTO;
import no.nav.veilarbvedtaksstotte.repository.UtrullingRepository;
import no.nav.veilarbvedtaksstotte.repository.domain.UtrulletEnhet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UtrullingService {

    private final UtrullingRepository utrullingRepository;

    private final VeilarbarenaClient veilarbarenaClient;

    private final VeilarbveilederClient veilarbveilederClient;

    private final Norg2Client norg2Client;

    @Autowired
    public UtrullingService(
            UtrullingRepository utrullingRepository,
            VeilarbarenaClient veilarbarenaClient,
            VeilarbveilederClient veilarbveilederClient,
            Norg2Client norg2Client
    ) {
        this.utrullingRepository = utrullingRepository;
        this.veilarbarenaClient = veilarbarenaClient;
        this.veilarbveilederClient = veilarbveilederClient;
        this.norg2Client = norg2Client;
    }

    public List<UtrulletEnhet> hentAlleUtrullinger() {
        return utrullingRepository.hentAlleUtrullinger();
    }

    public void leggTilUtrulling(EnhetId enhetId) {
        String enhetNavn = norg2Client.hentEnhet(enhetId.get()).getNavn();
        utrullingRepository.leggTilUtrulling(enhetId, enhetNavn);
    }

    public void fjernUtrulling(EnhetId enhetId) {
        utrullingRepository.fjernUtrulling(enhetId);
    }

    public boolean erUtrullet(EnhetId enhetId) {
        return utrullingRepository.erUtrullet(enhetId);
    }

    public boolean tilhorerBrukerUtrulletKontor(Fnr fnr) {
        try {
            log.error("OPP bruker: " + veilarbarenaClient.hentOppfolgingsbruker(fnr).map(VeilarbArenaOppfolging::getNavKontor));
            return veilarbarenaClient.hentOppfolgingsbruker(fnr)
                    .map(VeilarbArenaOppfolging::getNavKontor)
                    .map(EnhetId::of)
                    .map(utrullingRepository::erUtrullet)
                    .orElse(false);

        } catch (Exception e) {
            log.error("Feil med henting tilhorerBrukerUtrulletKontor: " + e, e);
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

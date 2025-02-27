package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.arena.dto.VeilarbArenaOppfolging;
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.VeilederEnheterDTO;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.UtrullingRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.repository.domain.UtrulletEnhet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UtrullingService {

    private final UtrullingRepository utrullingRepository;

    private final VeilarbarenaClient veilarbarenaClient;

    private final VeilarbveilederClient veilarbveilederClient;

    private final Norg2Client norg2Client;
    private final VedtaksstotteRepository vedtaksstotteRepository;
    private final AktorOppslagClient aktorOppslagClient;

    @Autowired
    public UtrullingService(
            UtrullingRepository utrullingRepository,
            VeilarbarenaClient veilarbarenaClient,
            VeilarbveilederClient veilarbveilederClient,
            Norg2Client norg2Client,
            VedtaksstotteRepository vedtaksstotteRepository, AktorOppslagClient aktorOppslagClient) {
        this.utrullingRepository = utrullingRepository;
        this.veilarbarenaClient = veilarbarenaClient;
        this.veilarbveilederClient = veilarbveilederClient;
        this.norg2Client = norg2Client;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.aktorOppslagClient = aktorOppslagClient;
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

    public void sjekkAtBrukerTilhorerUtrulletKontor(long vedtakId) {
        try {
            Fnr fnr = finnFodselsnummerFraVedtakId(vedtakId);
            sjekkAtBrukerTilhorerUtrulletKontor(fnr);
        } catch (ResponseStatusException responseStatusException) {
            throw responseStatusException;
        } catch (Exception e) {
            log.info("Greide ikke sjekke om vedtaksstøtte er utrullet for enheten til bruker. Tilgang er stoppet");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Greide ikke sjekke om vedtaksstøtte er utrullet for enheten til bruker");
        }
    }

    public void sjekkAtBrukerTilhorerUtrulletKontor(Fnr fnr) {
        if (!tilhorerBrukerUtrulletKontor(fnr)) {
            log.info("Vedtaksstøtte er ikke utrullet for enheten til bruker. Tilgang er stoppet");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vedtaksstøtte er ikke utrullet for enheten til bruker");
        }
    }

    private Fnr finnFodselsnummerFraVedtakId(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);

        if (vedtak == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke vedtak med vedtakId " + vedtakId);
        }

        AktorId aktorId = AktorId.of(vedtak.getAktorId());

        return aktorOppslagClient.hentFnr(aktorId);
    }
}

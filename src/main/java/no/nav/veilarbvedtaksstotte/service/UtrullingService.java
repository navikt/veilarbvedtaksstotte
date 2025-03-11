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

    public void sjekkAtBrukerTilhorerUtrulletKontor(Fnr fnr) {
        if (!tilhorerBrukerUtrulletKontor(fnr)) {
            log.info("Vedtaksstøtte er ikke utrullet for enheten til bruker. Tilgang er stoppet");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vedtaksstøtte er ikke utrullet for enheten til bruker");
        }
    }

    /**
     * Sjekkar om veileder skal ha tilgang til løysinga.
     * <p>
     * Køyrer utan unntak dersom minst ein av sjekkane fungerer:
     * - Løysinga er påskrudd på kontoret til innbyggaren det skal behandlast informasjon for. (UtrullingService)
     * - Løysinga er påskrudd for veileder. (Unleash) TODO implementer dette.
     * <p>
     * Utløyser unntak dersom:
     * - veileder ikkje skal ha tilgang til løysinga
     * <p>
     * ResponseStatusException(HttpStatus.NOT_FOUND) – ugyldig vedtakId
     * ResponseStatusException(HttpStatus.FORBIDDEN) – veileder skal ikkje ha tilgang.
     */
    public void sjekkOmMinstEnFeaturetoggleErPa(Fnr fnr) {
        sjekkAtBrukerTilhorerUtrulletKontor(fnr);
    }

    /**
     * Sjekkar om veileder skal ha tilgang til løysinga.
     * <p>
     * Køyrer utan unntak dersom minst ein av sjekkane fungerer:
     * - Løysinga er påskrudd på kontoret til innbyggaren det skal behandlast informasjon for. (UtrullingService)
     * - Løysinga er påskrudd for veileder. (Unleash) TODO implementer dette.
     * <p>
     * Utløyser unntak dersom:
     * - veileder ikkje skal ha tilgang til løysinga
     * - vedtakId er ugyldig eller ein ikkje finn fødselsnummer for innbyggar frå aktorId i vedtaket
     * <p>
     * @Exception IngenGjeldendeIdentException – Finn ikkje fødselsnummer frå aktorId i vedtaket som er sendt inn
     * ResponseStatusException(HttpStatus.NOT_FOUND) – ugyldig vedtakId
     * ResponseStatusException(HttpStatus.FORBIDDEN) – veileder skal ikkje ha tilgang.
     */
    public void sjekkOmMinstEnFeaturetoggleErPa(long vedtakId) {
        Fnr fnr = finnFodselsnummerFraVedtakId(vedtakId);
        sjekkOmMinstEnFeaturetoggleErPa(fnr);
    }


    private Fnr finnFodselsnummerFraVedtakId(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);

        if (vedtak == null) {
            log.info("Sjekk om innbygger tilhører utrullet kontor: Finner ikke vedtak for id, og derfor heller ikke tilhørende fnr. Tilgang er stoppet. VedtakId: " + vedtakId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke vedtak med vedtakId " + vedtakId);
        }

        AktorId aktorId = AktorId.of(vedtak.getAktorId());

        return aktorOppslagClient.hentFnr(aktorId);
    }
}

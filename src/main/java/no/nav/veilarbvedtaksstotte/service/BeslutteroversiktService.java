package no.nav.veilarbvedtaksstotte.service;

import io.getunleash.DefaultUnleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.EnhetNavn;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.PortefoljeEnhet;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.VeilederEnheterDTO;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.*;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.veilarbvedtaksstotte.utils.UnleashUtilsKt.TILGANGSMASKIN_INTEGRASJON;

@Service
@Slf4j
public class BeslutteroversiktService {

    private static final String VIKAFOSSEN_ENHET_ID = "2103";

    private final BeslutteroversiktRepository beslutteroversiktRepository;

    private final VeilarbveilederClient veiledereOgEnhetClient;

    private final AuthService authService;
    private final DefaultUnleash unleashService;

    @Autowired
    public BeslutteroversiktService(
            BeslutteroversiktRepository beslutteroversiktRepository,
            VeilarbveilederClient veiledereOgEnhetClient,
            AuthService authService,
            DefaultUnleash unleashService
    ) {
        this.beslutteroversiktRepository = beslutteroversiktRepository;
        this.veiledereOgEnhetClient = veiledereOgEnhetClient;
        this.authService = authService;
        this.unleashService = unleashService;
    }

    public BrukereMedAntall sokEtterBruker(BeslutteroversiktSok sok) {
        if (unleashService.isEnabled(TILGANGSMASKIN_INTEGRASJON)) {
            return sokEtterBrukerMedTilgangsmaskin(sok);
        } else {
            return sokEtterBrukerGammel(sok);
        }
    }

    public BrukereMedAntall sokEtterBrukerMedTilgangsmaskin(BeslutteroversiktSok sok) {
        if (sok.getFilter() == null) {
            sok.setFilter(new BeslutteroversiktSokFilter());
        }

        VeilederEnheterDTO veilederEnheterDTO = veiledereOgEnhetClient.hentInnloggetVeilederEnheter();
        List<String> veilederEnheter = veilederEnheterDTO.getEnhetliste()
                .stream()
                .map(PortefoljeEnhet::getEnhetId)
                .collect(Collectors.toList());

        List<String> enhetFilter = sok.getFilter().getEnheter();
        boolean erDev = isDevelopment().orElse(false);

        if (enhetFilter == null || enhetFilter.isEmpty()) {
            sok.getFilter().setEnheter(veilederEnheter);
        } else if (!erDev) {
            sjekkTilgangTilAlleEnheter(enhetFilter, veilederEnheter);
        }

        if (erDev
                && sok.getFilter().getEnheter().contains(VIKAFOSSEN_ENHET_ID)
                && !authService.harTilgangTilKode6Brukere()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Veileder mangler tilgang til kode 6-brukere");
        }

        if (sok.getFilter().getEnheter().isEmpty()) {
            return new BrukereMedAntall(List.of(), 0);
        }

        BrukereMedAntall brukereMedAntall = beslutteroversiktRepository.sokEtterBrukere(sok, veilederEnheterDTO.getIdent());
        sensurerBrukere(brukereMedAntall.getBrukere());

        return brukereMedAntall;
    }

    public BrukereMedAntall sokEtterBrukerGammel(BeslutteroversiktSok sok) {
        if (sok.getFilter() == null) {
            sok.setFilter(new BeslutteroversiktSokFilter());
        }

        VeilederEnheterDTO veilederEnheterDTO = veiledereOgEnhetClient.hentInnloggetVeilederEnheter();
        List<String> veilederEnheter = veilederEnheterDTO.getEnhetliste()
                .stream()
                .map(PortefoljeEnhet::getEnhetId)
                .collect(Collectors.toList());

        List<String> enhetFilter = sok.getFilter().getEnheter();
        if (enhetFilter == null || enhetFilter.isEmpty()) {
            sok.getFilter().setEnheter(veilederEnheter);
        } else {
            sjekkTilgangTilAlleEnheter(enhetFilter, veilederEnheter);
        }

        BrukereMedAntall brukereMedAntall = beslutteroversiktRepository.sokEtterBrukere(sok, veilederEnheterDTO.getIdent());
        sensurerBrukereGammel(brukereMedAntall.getBrukere());

        return brukereMedAntall;
    }

    public EnheterMedUtkast hentEnheterMedUtkast() {
        var veilederEnheter = veiledereOgEnhetClient.hentInnloggetVeilederEnheter().getEnhetliste()
                .stream()
                .map(PortefoljeEnhet::getEnhetId)
                .collect(Collectors.toSet());

        List<EnhetNavn> medTilgang = new ArrayList<>();
        List<EnhetNavn> utenTilgang = new ArrayList<>();

        for (String enhetId : beslutteroversiktRepository.hentEnheterMedUtkast()) {
            if (VIKAFOSSEN_ENHET_ID.equals(enhetId) && !authService.harTilgangTilKode6Brukere()) {
                continue;
            }
            EnhetNavn enhet = new EnhetNavn(enhetId, veiledereOgEnhetClient.hentEnhetNavn(enhetId));
            if (veilederEnheter.contains(enhetId)) {
                medTilgang.add(enhet);
            } else {
                utenTilgang.add(enhet);
            }
        }

        return new EnheterMedUtkast(medTilgang, utenTilgang);
    }

    private void sjekkTilgangTilAlleEnheter(List<String> sokteEnheter, List<String> veilederEnheter) {
        if (!new HashSet<>(veilederEnheter).containsAll(sokteEnheter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Veileder mangler tilgang til enhet");
        }
    }

    void sensurerBrukere(List<BeslutteroversiktBruker> brukere) {
        if (unleashService.isEnabled(TILGANGSMASKIN_INTEGRASJON) && isDevelopment().orElse(false)) {
            sensurerBrukereMedKjerneregler(brukere);
        } else {
            sensurerBrukereGammel(brukere);
        }
    }

    void sensurerBrukereMedKjerneregler(List<BeslutteroversiktBruker> brukere) {
        if (brukere == null || brukere.isEmpty()) {
            return;
        }

        List<String> brukerFnrs = brukere.stream().map(BeslutteroversiktBruker::getBrukerFnr).toList();
        Map<String, Boolean> tilgangTilBrukere = authService.harInnloggetVeilederTilgangTilBrukereMedKjerneregler(brukerFnrs);
        maskerUtenTilgang(brukere, tilgangTilBrukere);
    }

    void sensurerBrukereGammel(List<BeslutteroversiktBruker> brukere) {
        if (brukere == null || brukere.isEmpty()) {
            return;
        }

        List<String> brukerFnrs = brukere.stream().map(BeslutteroversiktBruker::getBrukerFnr).toList();
        Map<String, Boolean> tilgangTilBrukere = authService.harInnloggetVeilederTilgangTilBrukere(brukerFnrs);
        maskerUtenTilgang(brukere, tilgangTilBrukere);
    }

    private static void maskerUtenTilgang(List<BeslutteroversiktBruker> brukere, Map<String, Boolean> tilgangTilBrukere) {
        brukere.forEach(bruker -> {
            boolean harTilgang = tilgangTilBrukere.getOrDefault(bruker.getBrukerFnr(), Boolean.FALSE);

            if (!harTilgang) {
                fjernKonfidensiellInfo(bruker);
            }
        });
    }

    private static void fjernKonfidensiellInfo(BeslutteroversiktBruker bruker) {
        bruker.setBrukerFnr("");
        bruker.setBrukerFornavn("");
        bruker.setBrukerEtternavn("");
    }

}

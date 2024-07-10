package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.PortefoljeEnhet;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.VeilederEnheterDTO;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSokFilter;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BrukereMedAntall;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BeslutteroversiktService {

    private final BeslutteroversiktRepository beslutteroversiktRepository;

    private final VeilarbveilederClient veiledereOgEnhetClient;

    private final AuthService authService;

    @Autowired
    public BeslutteroversiktService(
            BeslutteroversiktRepository beslutteroversiktRepository,
            VeilarbveilederClient veiledereOgEnhetClient,
            AuthService authService
    ) {
        this.beslutteroversiktRepository = beslutteroversiktRepository;
        this.veiledereOgEnhetClient = veiledereOgEnhetClient;
        this.authService = authService;
    }

    public BrukereMedAntall sokEtterBruker(BeslutteroversiktSok sok) {
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
        sensurerBrukere(brukereMedAntall.getBrukere());

        return brukereMedAntall;
    }

    private void sjekkTilgangTilAlleEnheter(List<String> sokteEnheter, List<String> veilederEnheter) {
        if (!new HashSet<>(veilederEnheter).containsAll(sokteEnheter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Veileder mangler tilgang til enhet");
        }
    }

    void sensurerBrukere(List<BeslutteroversiktBruker> brukere) {
        if (brukere == null || brukere.isEmpty()) {
            return;
        }

        List<String> brukerFnrs = brukere.stream().map(BeslutteroversiktBruker::getBrukerFnr).toList();
        Map<String, Boolean> tilgangTilBrukere = authService.harInnloggetVeilederTilgangTilBrukere(brukerFnrs);

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

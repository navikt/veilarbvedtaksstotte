package no.nav.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.veilarbvedtaksstotte.client.VeiledereOgEnhetClient;
import no.nav.veilarbvedtaksstotte.domain.*;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BeslutteroversiktService {

    private final BeslutteroversiktRepository beslutteroversiktRepository;

    private final VeiledereOgEnhetClient veiledereOgEnhetClient;

    @Inject
    public BeslutteroversiktService(BeslutteroversiktRepository beslutteroversiktRepository, VeiledereOgEnhetClient veiledereOgEnhetClient) {
        this.beslutteroversiktRepository = beslutteroversiktRepository;
        this.veiledereOgEnhetClient = veiledereOgEnhetClient;
    }

    public List<BeslutteroversiktBruker> sokEtterBruker(BeslutteroversiktSok sok) {
        if (sok.getFilter() == null) {
            sok.setFilter(new BeslutteroversiktSokFilter());
        }

        VeilederEnheterDTO veilederEnheterDTO = veiledereOgEnhetClient.hentInnloggetVeilederEnheter();
        List<String> veilederEnheter = veilederEnheterDTO.getEnhetliste()
                .stream()
                .map(PortefoljeEnhet::getEnhetId)
                .collect(Collectors.toList());

        if (sok.getFilter().getEnheter() == null) {
            sok.getFilter().setEnheter(veilederEnheter);
        } else  {
            sjekkTilgangTilAlleEnheter(sok.getFilter().getEnheter(), veilederEnheter);
        }

        return beslutteroversiktRepository.sokEtterBrukere(sok);
    }

    private void sjekkTilgangTilAlleEnheter(List<String> sokteEnheter, List<String> veilederEnheter) {
        if (!veilederEnheter.containsAll(sokteEnheter)) {
            throw new IngenTilgang("Veileder mangler tilgang til enhet");
        }
    }

}

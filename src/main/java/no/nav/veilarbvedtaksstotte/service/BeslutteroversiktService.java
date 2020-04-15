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

    private final AuthService authService;

    @Inject
    public BeslutteroversiktService(
            BeslutteroversiktRepository beslutteroversiktRepository,
            VeiledereOgEnhetClient veiledereOgEnhetClient,
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
        if (!veilederEnheter.containsAll(sokteEnheter)) {
            throw new IngenTilgang("Veileder mangler tilgang til enhet");
        }
    }

    private void sensurerBrukere(List<BeslutteroversiktBruker> brukere) {
        brukere.forEach(this::fjernKonfidensiellInfoDersomIkkeTilgang);
    }

    private void fjernKonfidensiellInfoDersomIkkeTilgang(BeslutteroversiktBruker bruker) {
        String diskresjonskode = ""; // TODO: Hent fra bruker eller API
        boolean erEgenAnsatt = false; // TODO: Hent fra bruker eller API

        boolean manglerTilgang =
                ("6".equals(diskresjonskode) && !authService.harInnloggetVeilederTilgangTilKode6())
                || ("7".equals(diskresjonskode) && !authService.harInnloggetVeilederTilgangTilKode7())
                || (erEgenAnsatt && !authService.harInnloggetVeilederTilgangTilEgenAnsatt());

        if (manglerTilgang) {
            fjernKonfidensiellInfo(bruker);
        }
    }

    private static void fjernKonfidensiellInfo(BeslutteroversiktBruker bruker) {
        bruker.setBrukerFnr("");
        bruker.setBrukerFornavn("");
        bruker.setBrukerEtternavn("");
    }

}

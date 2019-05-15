package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static no.nav.brukerdialog.security.domain.IdentType.InternBruker;
import static no.nav.fo.veilarbvedtaksstotte.utils.AktorIdUtils.getAktorIdOrThrow;

@Service
public class AuthService {

    private AktorService aktorService;
    private VeilarbAbacPepClient pepClient;

    @Inject
    public AuthService(AktorService aktorService,
                       VeilarbAbacPepClient pepClient) {
        this.aktorService = aktorService;
        this.pepClient = pepClient;
    }


    public Bruker sjekkSkrivetilgangTilBruker(String fnr) {
        sjekkInternBruker();
        String aktorId = getAktorIdOrThrow(aktorService, fnr);
        Bruker bruker = Bruker.fraAktoerId(aktorId).medFoedselsnummer(fnr);
        pepClient.sjekkSkrivetilgangTilBruker(bruker);

        return bruker;
    }

    private void sjekkInternBruker() {
        SubjectHandler
                .getIdentType()
                .filter(InternBruker::equals)
                .orElseThrow(() -> new IngenTilgang("Ikke intern bruker"));
    }
}

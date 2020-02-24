package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbvedtaksstotte.client.ArenaClient;
import no.nav.fo.veilarbvedtaksstotte.domain.AuthKontekst;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static no.nav.brukerdialog.security.domain.IdentType.InternBruker;

@Service
public class AuthService {

    private final AktorService aktorService;
    private final PepClient pepClient;
    private final ArenaClient arenaClient;

    @Inject
    public AuthService(AktorService aktorService,
                       PepClient pepClient,
                       ArenaClient arenaClient) {
        this.aktorService = aktorService;
        this.pepClient = pepClient;
        this.arenaClient = arenaClient;
    }


    public AuthKontekst sjekkTilgang(String fnr) {
        sjekkInternBruker();

        String aktorId = getAktorIdOrThrow(fnr);

        pepClient.sjekkSkrivetilgangTilAktorId(aktorId);
        String enhet = sjekkTilgangTilEnhet(fnr);

        return new AuthKontekst()
                .setFnr(fnr)
                .setAktorId(aktorId)
                .setOppfolgingsenhet(enhet);
    }

    private void sjekkInternBruker() {
        SubjectHandler
                .getIdentType()
                .filter(InternBruker::equals)
                .orElseThrow(() -> new IngenTilgang("Ikke intern bruker"));
    }

    private String sjekkTilgangTilEnhet(String fnr) {
        String enhet = arenaClient.oppfolgingsenhet(fnr);
        if(!pepClient.harTilgangTilEnhet(enhet)) {
            throw new IngenTilgang("Ikke tilgang til enhet");
        }
        return enhet;
    }

    private String getAktorIdOrThrow(String fnr) {
        return aktorService.getAktorId(fnr)
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr"));
    }
}

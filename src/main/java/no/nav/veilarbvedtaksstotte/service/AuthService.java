package no.nav.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.veilarbvedtaksstotte.client.ArenaClient;
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static no.nav.brukerdialog.security.domain.IdentType.InternBruker;

@Service
public class AuthService {

    private final AktorService aktorService;
    private final PepClient pepClient;
    private final ArenaClient arenaClient;
    private final VeilederService veilederService;

    @Inject
    public AuthService(AktorService aktorService,
                       PepClient pepClient,
                       ArenaClient arenaClient,
                       VeilederService veilederService) {
        this.aktorService = aktorService;
        this.pepClient = pepClient;
        this.arenaClient = arenaClient;
        this.veilederService = veilederService;
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

    public String getInnloggetVeilederIdent() {
        return SubjectHandler
                .getIdent()
                .orElseThrow(() -> new IngenTilgang("Fant ikke ident for innlogget veileder"));
    }

    public String getFnrOrThrow(String aktorId) {
        return aktorService.getFnr(aktorId)
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke fnr for aktørId"));
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

    public void sjekkAnsvarligVeileder(Vedtak vedtak) {
        if (!vedtak.getVeilederIdent().equals(veilederService.hentVeilederIdentFraToken())) {
            throw new IngenTilgang("Ikke ansvarlig veileder.");
        }
    }
}

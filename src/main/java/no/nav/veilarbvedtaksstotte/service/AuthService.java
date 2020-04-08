package no.nav.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.veilarbvedtaksstotte.client.ArenaClient;
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static no.nav.brukerdialog.security.domain.IdentType.InternBruker;

@Service
public class AuthService {

    private final static String ABAC_VEILARB_DOMAIN = "veilarb";

    private final AktorService aktorService;
    private final PepClient pepClient;
    private final Pep pep;
    private final ArenaClient arenaClient;

    @Inject
    public AuthService(AktorService aktorService,
                       PepClient pepClient,
                       Pep pep,
                       ArenaClient arenaClient) {
        this.aktorService = aktorService;
        this.pepClient = pepClient;
        this.pep = pep;
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

    public String getInnloggetVeilederIdent() {
        return SubjectHandler
                .getIdent()
                .orElseThrow(() -> new IngenTilgang("Fant ikke ident for innlogget veileder"));
    }

    public String getFnrOrThrow(String aktorId) {
        return aktorService.getFnr(aktorId)
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke fnr for aktørId"));
    }

    public void sjekkAnsvarligVeileder(Vedtak vedtak) {
        if (!vedtak.getVeilederIdent().equals(getInnloggetVeilederIdent())) {
            throw new IngenTilgang("Ikke ansvarlig veileder.");
        }
    }

    public boolean harInnloggetVeilederTilgangTilKode6() {
        String token = getInnloggetVeilederToken();
        return isPermitted(pep.isSubjectAuthorizedToSeeKode6(token, ABAC_VEILARB_DOMAIN));
    }

    public boolean harInnloggetVeilederTilgangTilKode7() {
        String token = getInnloggetVeilederToken();
        return isPermitted(pep.isSubjectAuthorizedToSeeKode7(token, ABAC_VEILARB_DOMAIN));
    }

    public boolean harInnloggetVeilederTilgangTilEgenAnsatt() {
        String token = getInnloggetVeilederToken();
        return isPermitted(pep.isSubjectAuthorizedToSeeEgenAnsatt(token, ABAC_VEILARB_DOMAIN));
    }

    private boolean isPermitted(BiasedDecisionResponse decision) {
        return decision.getBiasedDecision() == Decision.Permit;
    }

    private void sjekkInternBruker() {
        SubjectHandler
                .getIdentType()
                .filter(InternBruker::equals)
                .orElseThrow(() -> new IngenTilgang("Ikke intern bruker"));
    }

    private String getInnloggetVeilederToken() {
        return SubjectHandler
                .getSsoToken()
                .map(SsoToken::getToken)
                .orElseThrow(() -> new IngenTilgang("Fant ikke token for innlogget veileder"));
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

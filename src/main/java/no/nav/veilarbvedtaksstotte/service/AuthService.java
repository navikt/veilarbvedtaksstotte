package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.abac.AbacClient;
import no.nav.common.abac.Pep;
import no.nav.common.abac.constants.NavAttributter;
import no.nav.common.abac.constants.StandardAttributter;
import no.nav.common.abac.domain.Attribute;
import no.nav.common.abac.domain.request.*;
import no.nav.common.abac.domain.response.Category;
import no.nav.common.abac.domain.response.Decision;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.Credentials;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Service
public class AuthService {

    private final AktorregisterClient aktorregisterClient;
    private final Pep veilarbPep;
    private final VeilarbarenaClient arenaClient;
    private final AbacClient abacClient;
    private final Credentials serviceUserCredentials;

    @Autowired
    public AuthService(
            AktorregisterClient aktorregisterClient,
            Pep veilarbPep,
            VeilarbarenaClient arenaClient,
            AbacClient abacClient,
            Credentials serviceUserCredentials
    ) {
        this.aktorregisterClient = aktorregisterClient;
        this.veilarbPep = veilarbPep;
        this.arenaClient = arenaClient;
        this.abacClient = abacClient;
        this.serviceUserCredentials = serviceUserCredentials;
    }

    public AuthKontekst sjekkTilgangTilFnr(String fnr) {
        sjekkInternBruker();

        AktorId aktorId = aktorregisterClient.hentAktorId(Fnr.of(fnr));

        if (!veilarbPep.harVeilederTilgangTilPerson(NavIdent.of(getInnloggetVeilederIdent()), ActionId.WRITE, aktorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String enhet = sjekkTilgangTilEnhet(fnr);

        return new AuthKontekst()
                .setFnr(fnr)
                .setAktorId(aktorId.get())
                .setOppfolgingsenhet(enhet);
    }

    public AuthKontekst sjekkTilgangTilAktorId(String aktorId) {
        sjekkInternBruker();

        String fnr = aktorregisterClient.hentFnr(AktorId.of(aktorId)).get();

        if (!veilarbPep.harVeilederTilgangTilPerson(NavIdent.of(getInnloggetVeilederIdent()), ActionId.WRITE, AktorId.of(aktorId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String enhet = sjekkTilgangTilEnhet(fnr);

        return new AuthKontekst()
                .setFnr(fnr)
                .setAktorId(aktorId)
                .setOppfolgingsenhet(enhet);
    }

    public String getInnloggetBrukerToken() {
        return AuthContextHolder
                .getIdTokenString()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bruker mangler token"));
    }

    public String getInnloggetVeilederIdent() {
        return AuthContextHolder
                .getNavIdent()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fant ikke ident for innlogget veileder"))
                .get();
    }

    public String getFnrOrThrow(String aktorId) {
        return aktorregisterClient.hentFnr(AktorId.of(aktorId)).get();
    }

    public void sjekkErAnsvarligVeilederFor(Vedtak vedtak) {
        if (!vedtak.getVeilederIdent().equals(getInnloggetVeilederIdent())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ikke ansvarlig veileder.");
        }
    }

    public Map<String, Boolean> harInnloggetVeilederTilgangTilBrukere(List<String> brukerFnrs) {
        XacmlRequest request = lagSjekkTilgangRequest(serviceUserCredentials.username, getInnloggetVeilederIdent(), brukerFnrs);
        XacmlResponse abacResponse = abacClient.sendRequest(request);
        return mapBrukerTilgangRespons(abacResponse);
    }

    XacmlRequest lagSjekkTilgangRequest(String systembrukerNavn, String veilederIdent, List<String> brukerFnrs) {
        Environment environment = new Environment();
        environment.addAttribute(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, systembrukerNavn));

        Action action = new Action();
        action.addAttribute(new Attribute(StandardAttributter.ACTION_ID, ActionId.WRITE.name()));

        AccessSubject accessSubject = new AccessSubject();
        accessSubject.addAttribute(new Attribute(StandardAttributter.SUBJECT_ID, veilederIdent));
        accessSubject.addAttribute(new Attribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, "InternBruker"));

        List<Resource> resources = brukerFnrs.stream()
                .map(this::mapBrukerFnrTilAbacResource)
                .collect(Collectors.toList());

        Request request = new Request()
                .withEnvironment(environment)
                .withAction(action)
                .withAccessSubject(accessSubject)
                .withResources(resources);

        return new XacmlRequest().withRequest(request);
    }

    private Resource mapBrukerFnrTilAbacResource(String fnr) {
        Resource resource = new Resource();
        resource.addAttribute(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, "veilarb"));
        resource.addAttribute(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_VEILARB_PERSON));
        resource.addAttribute(new Attribute(NavAttributter.RESOURCE_FELLES_PERSON_FNR, fnr, true));
        return resource;
    }

    Map<String, Boolean> mapBrukerTilgangRespons(XacmlResponse xacmlResponse) {
        Map<String, Boolean> tilgangTilBrukere = new HashMap<>();

        xacmlResponse.getResponse().forEach(response -> {
            boolean harTilgang = response.getDecision() == Decision.Permit;

            // There should always be a single category
            Category category = response.getCategory().get(0);
            String brukerFnr = category.getAttribute().getValue();

            tilgangTilBrukere.put(brukerFnr, harTilgang);
        });

        return tilgangTilBrukere;
    }

    private void sjekkInternBruker() {
        AuthContextHolder
                .getRole()
                .filter(role -> role == UserRole.INTERN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Ikke intern bruker"));
    }

    private String sjekkTilgangTilEnhet(String fnr) {
        EnhetId enhet = ofNullable(arenaClient.oppfolgingsenhet(Fnr.of(fnr))).orElse(EnhetId.of(""));

        if (!veilarbPep.harVeilederTilgangTilEnhet(NavIdent.of(getInnloggetVeilederIdent()), enhet)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return enhet.get();
    }

}

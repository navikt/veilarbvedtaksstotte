package no.nav.veilarbvedtaksstotte.service;

import lombok.SneakyThrows;
import no.nav.common.abac.AbacClient;
import no.nav.common.abac.NavAttributter;
import no.nav.common.abac.Pep;
import no.nav.common.abac.StandardAttributter;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.Attribute;
import no.nav.common.abac.domain.request.*;
import no.nav.common.abac.domain.response.Category;
import no.nav.common.abac.domain.response.Decision;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.aktorregisterklient.AktorregisterKlient;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.veilarbvedtaksstotte.client.ArenaClient;
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.nav.common.auth.subject.IdentType.InternBruker;

@Service
public class AuthService {

    private final AktorregisterKlient aktorregisterKlient;
    private final Pep veilarbPep;
    private final ArenaClient arenaClient;
    private final AbacClient abacClient;

    @Autowired
    public AuthService(AktorregisterKlient aktorregisterKlient,
                       Pep veilarbPep,
                       ArenaClient arenaClient,
                       AbacClient abacClient) {
        this.aktorregisterKlient = aktorregisterKlient;
        this.veilarbPep = veilarbPep;
        this.arenaClient = arenaClient;
        this.abacClient = abacClient;
    }

    public AuthKontekst sjekkTilgang(String fnr) {
        sjekkInternBruker();

        String aktorId = aktorregisterKlient.hentAktorId(fnr);

        veilarbPep.sjekkVeilederTilgangTilBruker(getInnloggetVeilederIdent(), ActionId.WRITE, AbacPersonId.aktorId(aktorId));
        String enhet = sjekkTilgangTilEnhet(fnr);

        return new AuthKontekst()
                .setFnr(fnr)
                .setAktorId(aktorId)
                .setOppfolgingsenhet(enhet);
    }

    public String getInnloggetVeilederIdent() {
        return SubjectHandler
                .getIdent()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fant ikke ident for innlogget veileder"));
    }

    public String getFnrOrThrow(String aktorId) {
        return aktorregisterKlient.hentFnr(aktorId);
    }

    public void sjekkAnsvarligVeileder(Vedtak vedtak) {
        if (!vedtak.getVeilederIdent().equals(getInnloggetVeilederIdent())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ikke ansvarlig veileder.");
        }
    }

    public Map<String, Boolean> harInnloggetVeilederTilgangTilBrukere(List<String> brukerFnrs) {
        String veilederIdent = getInnloggetVeilederIdent();
        String systembrukerNavn = System.getProperty("StsSecurityConstants.SYSTEMUSER_USERNAME");
        XacmlResponse abacResponse = sjekkTilgangTilBrukere(systembrukerNavn, veilederIdent, brukerFnrs);
        return mapBrukerTilgangRespons(abacResponse);
    }

    @SneakyThrows
    private XacmlResponse sjekkTilgangTilBrukere(String systembrukerNavn, String veilederIdent, List<String> brukerFnrs) {
        XacmlRequest request = lagSjekkTilgangRequest(systembrukerNavn, veilederIdent, brukerFnrs);
        return abacClient.sendRequest(request);
    }

    XacmlRequest lagSjekkTilgangRequest(String systembrukerNavn, String veilederIdent, List<String> brukerFnrs) {
        Environment environment = new Environment();
        environment.addAttribute(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, systembrukerNavn));

        Action action = new Action();
        action.addAttribute(new Attribute(StandardAttributter.ACTION_ID, ActionId.WRITE.name()));

        AccessSubject accessSubject = new AccessSubject();
        accessSubject.addAttribute(new Attribute(StandardAttributter.SUBJECT_ID, veilederIdent));
        accessSubject.addAttribute(new Attribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, InternBruker.name()));

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
        SubjectHandler
                .getIdentType()
                .filter(InternBruker::equals)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Ikke intern bruker"));
    }

    private String sjekkTilgangTilEnhet(String fnr) {
        String enhet = arenaClient.oppfolgingsenhet(fnr);
        veilarbPep.sjekkVeilederTilgangTilEnhet(getInnloggetVeilederIdent(), enhet);
        return enhet;
    }

}

package no.nav.veilarbvedtaksstotte.service;

import lombok.SneakyThrows;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.sbl.dialogarena.common.abac.pep.NavAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.StandardAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.*;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Category;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.veilarbvedtaksstotte.client.ArenaClient;
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.nav.brukerdialog.security.domain.IdentType.InternBruker;

@Service
public class AuthService {

    private final AktorService aktorService;
    private final PepClient pepClient;
    private final AbacService abacService;
    private final ArenaClient arenaClient;

    @Inject
    public AuthService(AktorService aktorService,
                       PepClient pepClient,
                       AbacService abacService,
                       ArenaClient arenaClient) {
        this.aktorService = aktorService;
        this.pepClient = pepClient;
        this.abacService = abacService;
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

    public Map<String, Boolean> harInnloggetVeilederTilgangTilBrukere(List<String> brukerFnrs) {
        String veilederIdent = getInnloggetVeilederIdent();
        String systembrukerNavn = System.getProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
        XacmlResponse abacResponse = sjekkTilgangTilBrukere(systembrukerNavn, veilederIdent, brukerFnrs);
        return mapBrukerTilgangRespons(abacResponse);
    }

    @SneakyThrows
    private XacmlResponse sjekkTilgangTilBrukere(String systembrukerNavn, String veilederIdent, List<String> brukerFnrs) {
        XacmlRequest request = lagSjekkTilgangRequest(systembrukerNavn, veilederIdent, brukerFnrs);
        return abacService.askForPermission(request);
    }

    XacmlRequest lagSjekkTilgangRequest(String systembrukerNavn, String veilederIdent, List<String> brukerFnrs) {
        Environment environment = new Environment();
        environment.addAttribute(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, systembrukerNavn));

        Action action = new Action();
        action.addAttribute(new Attribute(StandardAttributter.ACTION_ID, Action.ActionId.WRITE.name()));

        AccessSubject accessSubject = new AccessSubject();
        accessSubject.addAttribute(new Attribute(StandardAttributter.SUBJECT_ID, veilederIdent));
        accessSubject.addAttribute(new Attribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, IdentType.InternBruker.name()));

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

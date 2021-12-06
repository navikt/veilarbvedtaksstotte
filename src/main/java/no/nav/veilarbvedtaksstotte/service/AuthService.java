package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
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
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.*;
import no.nav.common.utils.Credentials;
import no.nav.common.utils.Pair;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbArenaOppfolging;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
public class AuthService {

    private final AktorOppslagClient aktorOppslagClient;
    private final Pep veilarbPep;
    private final VeilarbarenaClient arenaClient;
    private final AbacClient abacClient;
    private final Credentials serviceUserCredentials;
    private final AuthContextHolder authContextHolder;
    private final UtrullingService utrullingService;

    @Autowired
    public AuthService(
            AktorOppslagClient aktorOppslagClient,
            Pep veilarbPep,
            VeilarbarenaClient arenaClient,
            AbacClient abacClient,
            Credentials serviceUserCredentials,
            AuthContextHolder authContextHolder,
            UtrullingService utrullingService) {
        this.aktorOppslagClient = aktorOppslagClient;
        this.veilarbPep = veilarbPep;
        this.arenaClient = arenaClient;
        this.abacClient = abacClient;
        this.serviceUserCredentials = serviceUserCredentials;
        this.authContextHolder = authContextHolder;
        this.utrullingService = utrullingService;
    }

    public void sjekkTilgangTilBruker(Fnr fnr) {
        sjekkTilgangTilBruker(() -> fnr, () -> aktorOppslagClient.hentAktorId(fnr));
    }

    public void sjekkTilgangTilBruker(AktorId aktorId) {
        sjekkTilgangTilBruker(() -> aktorOppslagClient.hentFnr(aktorId), () -> aktorId);
    }

    public AuthKontekst sjekkTilgangTilBrukerOgEnhet(Fnr fnr) {
        return sjekkTilgangTilBrukerOgEnhet(() -> fnr, () -> aktorOppslagClient.hentAktorId(fnr));
    }

    public AuthKontekst sjekkTilgangTilBrukerOgEnhet(AktorId aktorId) {
        return sjekkTilgangTilBrukerOgEnhet(() -> aktorOppslagClient.hentFnr(aktorId), () -> aktorId);
    }

    private Pair<Fnr, AktorId> sjekkTilgangTilBruker(Supplier<Fnr> fnrSupplier, Supplier<AktorId> aktorIdSupplier) {
        sjekkInternBruker();

        Fnr fnr = fnrSupplier.get();
        AktorId aktorId = aktorIdSupplier.get();

        if (!veilarbPep.harVeilederTilgangTilPerson(NavIdent.of(getInnloggetVeilederIdent()), ActionId.WRITE, aktorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return Pair.of(fnr, aktorId);
    }

    private AuthKontekst sjekkTilgangTilBrukerOgEnhet(Supplier<Fnr> fnrSupplier, Supplier<AktorId> aktorIdSupplier) {

        Pair<Fnr, AktorId> fnrAktorIdPair = sjekkTilgangTilBruker(fnrSupplier, aktorIdSupplier);

        Fnr fnr = fnrAktorIdPair.getFirst();
        AktorId aktorId = fnrAktorIdPair.getSecond();

        String enhet = sjekkTilgangTilEnhet(fnr.get());

        return new AuthKontekst()
                .setFnr(fnr.get())
                .setAktorId(aktorId.get())
                .setOppfolgingsenhet(enhet);
    }

    public String getInnloggetBrukerToken() {
        return authContextHolder
                .getIdTokenString()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bruker mangler token"));
    }

    public String getInnloggetVeilederIdent() {
        return authContextHolder
                .getNavIdent()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fant ikke ident for innlogget veileder"))
                .get();
    }

    public Fnr getFnrOrThrow(String aktorId) {
        return aktorOppslagClient.hentFnr(AktorId.of(aktorId));
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
        authContextHolder
                .getRole()
                .filter(role -> role == UserRole.INTERN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Ikke intern bruker"));
    }

    private String sjekkTilgangTilEnhet(String fnr) {
        EnhetId enhet = ofNullable(arenaClient.hentOppfolgingsbruker(Fnr.of(fnr)))
                .map(VeilarbArenaOppfolging::getNavKontor)
                .map(EnhetId::of).orElse(EnhetId.of(""));

        if (!utrullingService.erUtrullet(enhet)) {
            log.info("Vedtaksstøtte er ikke utrullet for enhet {}. Tilgang er stoppet", enhet);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vedtaksstøtte er ikke utrullet for enheten");
        }

        if (!veilarbPep.harVeilederTilgangTilEnhet(NavIdent.of(getInnloggetVeilederIdent()), enhet)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return enhet.get();
    }
}

package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.Oppfolgingsenhet;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class ArenaClient extends BaseClient {

    public static final String VEILARBARENA_API_PROPERTY_NAME = "VEILARBARENA_URL";
    public static final String VEILARBARENA = "veilarbarena";

    @Inject
    public ArenaClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        super(getRequiredProperty(VEILARBARENA_API_PROPERTY_NAME), httpServletRequestProvider);
    }
    public String oppfolgingsenhet(String fnr) {
        return get(joinPaths(baseUrl, "api", "oppfolgingsbruker", fnr), Oppfolgingsenhet.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot veilarbarena/oppfolgingsbruker"))
                .getNavKontor();
    }

}

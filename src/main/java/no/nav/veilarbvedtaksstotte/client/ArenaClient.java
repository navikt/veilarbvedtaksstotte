package no.nav.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.Oppfolgingsenhet;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class ArenaClient extends BaseClient {

    public static final String VEILARBARENA_API_PROPERTY_NAME = "VEILARBARENA_URL";
    public static final String VEILARBARENA = "veilarbarena";

    public ArenaClient() {
        super(getRequiredProperty(VEILARBARENA_API_PROPERTY_NAME));
    }

    public String oppfolgingsenhet(String fnr) {
        return get(joinPaths(baseUrl, "api", "oppfolgingsbruker", fnr), Oppfolgingsenhet.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot veilarbarena/oppfolgingsbruker"))
                .getNavKontor();
    }

}

package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.EnhetNavn;
import no.nav.fo.veilarbvedtaksstotte.domain.VeilederePaEnhetDTO;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class VeiledereOgEnhetClient extends BaseClient {

    public static final String VEILARBVEILEDER_API_PROPERTY_NAME = "VEILARBVEILEDERAPI_URL";
    public static final String VEILARBVEILEDER = "veilarbveileder";

    @Inject
    public VeiledereOgEnhetClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        super(getRequiredProperty(VEILARBVEILEDER_API_PROPERTY_NAME), httpServletRequestProvider);
    }

    public String hentEnhetNavn(String enhetId) {
        return get(joinPaths(baseUrl, "api", "enhet", enhetId, "navn"), EnhetNavn.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot /veilarbveileder/api/enhet/{enhetId}/navn"))
                .getNavn();
    }

    public VeilederePaEnhetDTO hentVeilederePaEnhet(String enhetId) {
        return get(joinPaths(baseUrl, "api", "enhet", enhetId, "veiledere"), VeilederePaEnhetDTO.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot /veilarbveileder/api/enhet/{enhetId}/veiledere"));
    }

}


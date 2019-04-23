package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.AktivEnhetDTO;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class ModiaContextClient extends BaseClient {

    public static final String MODIA_CONTEXT_API_PROPERTY_NAME = "MODIACONTEXTHOLDERAPI_URL";
    public static final String MODIA_CONTEXT_HOLDER = "modiacontextholder";

    @Inject
    public ModiaContextClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        super(getRequiredProperty(MODIA_CONTEXT_API_PROPERTY_NAME), httpServletRequestProvider);
    }
    public String aktivEnhet() {
        return get(joinPaths(baseUrl, "api", "context", "aktivenhet"), AktivEnhetDTO.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot modiacontextholder/aktivenhet"))
                .getAktivEnhet();
    }

}

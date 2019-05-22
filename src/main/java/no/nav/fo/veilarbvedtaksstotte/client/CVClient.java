package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.utils.JsonUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class CVClient extends BaseClient {

    public static final String CV_API_PROPERTY_NAME = "CV_API_URL";
    public static final String PAM_CV_API = "pam-cv-api";

    @Inject
    public CVClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        super(getRequiredProperty(CV_API_PROPERTY_NAME), httpServletRequestProvider);
    }

    public String hentCV(String fnr) {
        RestResponse<String> response = get(joinPaths(baseUrl, "rest", "v1", "arbeidssoker", fnr), String.class);

        if (response.hasStatus(403)) {
            return JsonUtils.createErrorStr("Bruker har ikke delt CV/jobbprofil med NAV");
        }

        if (response.hasStatus(204)) {
            return JsonUtils.createNoDataStr("Bruker har ikke fylt ut CV/jobbprofil");
        }

        return response.withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot pam-cv-api/rest/v1/arbeidssoker/{fnr}"));
    }
}

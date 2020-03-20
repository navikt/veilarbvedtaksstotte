package no.nav.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class CVClient extends BaseClient {

    public static final String CV_API_PROPERTY_NAME = "CV_API_URL";
    public static final String PAM_CV_API = "pam-cv-api";

    public CVClient() {
        super(getRequiredProperty(CV_API_PROPERTY_NAME));
    }

    public String hentCV(String fnr) {
        RestResponse<String> response = get(joinPaths(baseUrl, "rest", "v1", "arbeidssoker", fnr), String.class);

        if (response.hasStatus(403) || response.hasStatus(401)) {
            return JsonUtils.createErrorStr("Bruker har ikke delt CV/jobbprofil med NAV");
        }

        if (response.hasStatus(204) || response.hasStatus(404)) {
            return JsonUtils.createNoDataStr("Bruker har ikke fylt ut CV/jobbprofil");
        }

        return response.withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot pam-cv-api/rest/v1/arbeidssoker/{fnr}"));
    }
}
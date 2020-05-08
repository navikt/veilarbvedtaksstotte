package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestClient;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

@Slf4j
@Component
public class CVClient {

    public static final String CV_API_PROPERTY_NAME = "CV_API_URL";
    public static final String PAM_CV_API = "pam-cv-api";

    private final String pamCvApiUrl;

    public CVClient() {
        this.pamCvApiUrl = getRequiredProperty(CV_API_PROPERTY_NAME);
    }

    @SneakyThrows
    public String hentCV(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(pamCvApiUrl, "/rest/v1/arbeidssoker/", fnr))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {

            if (response.code() == 403 || response.code() == 401) {
                return JsonUtils.createErrorStr("Bruker har ikke delt CV/jobbprofil med NAV");
            }

            if (response.code() == 204 || response.code() == 404) {
                return JsonUtils.createNoDataStr("Bruker har ikke fylt ut CV/jobbprofil");
            }

            return response.body().string();
        }
    }
}

package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestClient;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import no.nav.veilarbvedtaksstotte.utils.RestClientUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

@Slf4j
@Component
public class EgenvurderingClient {

    public static final String EGENVURDERING_API_PROPERTY_NAME = "EGENVURDERING_URL";
    public static final String VEILARBVEDTAKINFO = "veilarbvedtakinfo";

    private final String veilarbvedtakInfoUrl;

    public EgenvurderingClient() {
        this.veilarbvedtakInfoUrl = getRequiredProperty(EGENVURDERING_API_PROPERTY_NAME);
    }

    @SneakyThrows
    public String hentEgenvurdering(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbvedtakInfoUrl, "/api/behovsvurdering/besvarelse?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestClientUtils.throwIfNotSuccessful(response);

            if (response.code() == 204) {
                return JsonUtils.createNoDataStr("Bruker har ikke fylt ut egenvurdering");
            }

            return response.body().string();
        }
    }
}

package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import no.nav.common.rest.client.RestClient;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import no.nav.veilarbvedtaksstotte.utils.RestClientUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class EgenvurderingClient {

    private final String veilarbvedtakInfoUrl;

    public EgenvurderingClient(String veilarbvedtakInfoUrl) {
        this.veilarbvedtakInfoUrl = veilarbvedtakInfoUrl;
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

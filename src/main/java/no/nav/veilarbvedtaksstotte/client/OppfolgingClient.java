package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.domain.OppfolgingDTO;
import no.nav.veilarbvedtaksstotte.domain.OppfolgingstatusDTO;
import no.nav.veilarbvedtaksstotte.utils.RestClientUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class OppfolgingClient {

    private final String veilarboppfolgingUrl;

    public OppfolgingClient(String veilarboppfolgingUrl) {
        this.veilarboppfolgingUrl = veilarboppfolgingUrl;
    }

    @SneakyThrows
    public String hentServicegruppe(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/person/", fnr, "/oppfolgingsstatus"))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestClientUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseBodyOrThrow(response.body(), OppfolgingstatusDTO.class).getServicegruppe();
        }
    }

//    @Cacheable(CacheConfig.OPPFOLGING_CACHE_NAME)
    @SneakyThrows
    public OppfolgingDTO hentOppfolgingData(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/oppfolging?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestClientUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseBodyOrThrow(response.body(), OppfolgingDTO.class);
        }
    }
}

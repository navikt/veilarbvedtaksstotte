package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.domain.OppfolgingDTO;
import no.nav.veilarbvedtaksstotte.domain.OppfolgingstatusDTO;
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
public class OppfolgingClient {

    public static final String VEILARBOPPFOLGING_API_PROPERTY_NAME = "VEILARBOPPFOLGINGAPI_URL";
    public static final String VEILARBOPPFOLGING = "veilarboppfolging";

    private final String veilarboppfolgingUrl;

    public OppfolgingClient() {
        this.veilarboppfolgingUrl = getRequiredProperty(VEILARBOPPFOLGING_API_PROPERTY_NAME);
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

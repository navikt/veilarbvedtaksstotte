package no.nav.veilarbvedtaksstotte.client.arena;

import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.config.CacheConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class ArenaClientImpl implements ArenaClient {

    private final String veilarbarenaUrl;

    private final OkHttpClient client;

    public ArenaClientImpl(String veilarbarenaUrl) {
        this.veilarbarenaUrl = veilarbarenaUrl;
        this.client = RestClient.baseClient();
    }

    @Cacheable(CacheConfig.BRUKER_ENHET_CACHE_NAME)
    public String oppfolgingsenhet(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "/api/oppfolgingsbruker/", fnr))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, Oppfolgingsenhet.class).getNavKontor();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot veilarbarena/oppfolgingsbruker");
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbarenaUrl, "/internal/isReady"), client);
    }

}

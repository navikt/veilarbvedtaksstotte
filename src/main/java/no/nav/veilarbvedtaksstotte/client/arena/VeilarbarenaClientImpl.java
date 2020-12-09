package no.nav.veilarbvedtaksstotte.client.arena;

import lombok.Value;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.config.CacheConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.common.rest.client.RestUtils.parseJsonResponseOrThrow;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class VeilarbarenaClientImpl implements VeilarbarenaClient {

    private final String veilarbarenaUrl;

    private final OkHttpClient client;

    public VeilarbarenaClientImpl(String veilarbarenaUrl) {
        this.veilarbarenaUrl = veilarbarenaUrl;
        this.client = RestClient.baseClient();
    }

    @Cacheable(CacheConfig.BRUKER_ENHET_CACHE_NAME)
    public EnhetId oppfolgingsenhet(Fnr fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "/api/oppfolgingsbruker/", fnr.get()))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            String navKontor = parseJsonResponseOrThrow(response, Oppfolgingsenhet.class).getNavKontor();
            return EnhetId.of(navKontor);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot veilarbarena/oppfolgingsbruker");
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbarenaUrl, "/internal/isReady"), client);
    }

    @Override
    public String oppfolgingssak(Fnr fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "api", "oppfolgingssak", fnr.get()))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return parseJsonResponseOrThrow(response, ArenaOppfolgingssak.class).getOppfolgingssakId();
        } catch (Exception e) {
            throw new IllegalStateException("Fant ikke oppfolgingssak i Arena for bruker.", e);
        }
    }

    @Value
    public static class ArenaOppfolgingssak {
        String oppfolgingssakId;
    }
}

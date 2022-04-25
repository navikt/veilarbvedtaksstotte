package no.nav.veilarbvedtaksstotte.client.norg2;

import no.nav.common.client.norg2.CachedNorg2Client;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.client.norg2.NorgHttp2Client;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.EnhetId;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.config.CacheConfig.NORG2_ENHET_KONTAKTINFO_CACHE_NAME;
import static no.nav.veilarbvedtaksstotte.config.CacheConfig.NORG2_ENHET_ORGANISERING_CACHE_NAME;

public class Norg2ClientImpl implements Norg2Client {

    private final OkHttpClient client;
    private final String host;
    private final no.nav.common.client.norg2.Norg2Client norg2Client;

    public Norg2ClientImpl(String norg2Url) {
        this.client = RestClient.baseClient();
        host = norg2Url;
        norg2Client = new CachedNorg2Client(new NorgHttp2Client(norg2Url, client));
    }

    @Override
    public Enhet hentEnhet(String enhetId) {
        return norg2Client.hentEnhet(enhetId);
    }

    @Override
    public List<Enhet> hentAktiveEnheter() {
        return norg2Client.alleAktiveEnheter();
    }

    @Cacheable(NORG2_ENHET_KONTAKTINFO_CACHE_NAME)
    public EnhetKontaktinformasjon hentKontaktinfo(EnhetId enhetId) {
        Request request = new Request.Builder()
                .url(joinPaths(host, String.format("/api/v1/enhet/%s/kontaktinformasjon", enhetId)))
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, EnhetKontaktinformasjon.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot " + request.url().toString(), e);
        }
    }

    @Cacheable(NORG2_ENHET_ORGANISERING_CACHE_NAME)
    public List<EnhetOrganisering> hentEnhetOrganisering(EnhetId enhetId) {

        Request request = new Request.Builder()
                .url(joinPaths(host, String.format("/api/v1/enhet/%s/organisering", enhetId)))
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseArrayOrThrow(response, EnhetOrganisering.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot " + request.url(), e);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return norg2Client.checkHealth();
    }
}

package no.nav.veilarbvedtaksstotte.client.veilederogenhet;

import lombok.SneakyThrows;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.utils.UrlUtils;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.EnhetNavn;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.Veileder;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.VeilederEnheterDTO;
import no.nav.veilarbvedtaksstotte.config.CacheConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;

import java.util.function.Supplier;

import static no.nav.common.utils.UrlUtils.joinPaths;

public class VeilarbveilederClientImpl implements VeilarbveilederClient {

    private final String veilarbveilederUrl;

    private final OkHttpClient client;

    private final AuthContextHolder authContextHolder;
    private final Supplier<String> userTokenProvider;

    public VeilarbveilederClientImpl(String veilarbveilederUrl, AuthContextHolder authContextHolder, Supplier<String> userTokenProvider) {
        this.veilarbveilederUrl = veilarbveilederUrl;
        this.client = RestClient.baseClient();
        this.authContextHolder = authContextHolder;
        this.userTokenProvider = userTokenProvider;
    }

    @Cacheable(CacheConfig.ENHET_NAVN_CACHE_NAME)
    @SneakyThrows
    public String hentEnhetNavn(String enhetId) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(veilarbveilederUrl, "/api/enhet/", enhetId, "/navn"))
                .header(HttpHeaders.AUTHORIZATION, userTokenProvider.get())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, EnhetNavn.class).getNavn();
        }
    }

    @Cacheable(CacheConfig.VEILEDER_CACHE_NAME)
    @SneakyThrows
    public Veileder hentVeileder(String veilederIdent) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(veilarbveilederUrl, "/api/veileder/", veilederIdent))
                .header(HttpHeaders.AUTHORIZATION, userTokenProvider.get())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, Veileder.class);
        }
    }

    public VeilederEnheterDTO hentInnloggetVeilederEnheter() {
        String veilederIdent = authContextHolder
                .getNavIdent()
                .orElseThrow(() -> new IllegalStateException("Fant ikke veileder ident"))
                .get();

        return hentInnloggetVeilederEnheter(veilederIdent);
    }

    @Cacheable(CacheConfig.VEILEDER_ENHETER_CACHE_NAME)
    @SneakyThrows
    public VeilederEnheterDTO hentInnloggetVeilederEnheter(String veilederIdentUsedOnlyForCaching) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(veilarbveilederUrl, "/api/veileder/enheter"))
                .header(HttpHeaders.AUTHORIZATION, userTokenProvider.get())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, VeilederEnheterDTO.class);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbveilederUrl, "/internal/isReady"), client);
    }

}


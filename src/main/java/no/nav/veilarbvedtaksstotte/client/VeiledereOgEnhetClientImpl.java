package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.utils.UrlUtils;
import no.nav.veilarbvedtaksstotte.client.api.VeiledereOgEnhetClient;
import no.nav.veilarbvedtaksstotte.config.CacheConfig;
import no.nav.veilarbvedtaksstotte.domain.EnhetNavn;
import no.nav.veilarbvedtaksstotte.domain.Veileder;
import no.nav.veilarbvedtaksstotte.domain.VeilederEnheterDTO;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class VeiledereOgEnhetClientImpl implements VeiledereOgEnhetClient {

    private final String veilarbveilederUrl;

    private final OkHttpClient client;

    public VeiledereOgEnhetClientImpl(String veilarbveilederUrl) {
        this.veilarbveilederUrl = veilarbveilederUrl;
        this.client = RestClient.baseClient();
    }

    @Cacheable(CacheConfig.ENHET_NAVN_CACHE_NAME)
    @SneakyThrows
    public String hentEnhetNavn(String enhetId) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(veilarbveilederUrl, "/api/enhet/", enhetId, "/navn"))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseBodyOrThrow(response.body(), EnhetNavn.class).getNavn();
        }
    }

    @Cacheable(CacheConfig.VEILEDER_CACHE_NAME)
    @SneakyThrows
    public Veileder hentVeileder(String veilederIdent) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(veilarbveilederUrl, "/api/veileder/", veilederIdent))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseBodyOrThrow(response.body(), Veileder.class);
        }
    }

    public VeilederEnheterDTO hentInnloggetVeilederEnheter() {
        String veilederIdent = SubjectHandler.getIdent().orElseThrow(() -> new IllegalStateException("Fant ikke veileder ident"));
        return hentInnloggetVeilederEnheter(veilederIdent);
    }

    @Cacheable(CacheConfig.VEILEDER_ENHETER_CACHE_NAME)
    @SneakyThrows
    public VeilederEnheterDTO hentInnloggetVeilederEnheter(String veilederIdentUsedOnlyForCaching) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(veilarbveilederUrl, "/api/veileder/enheter"))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseBodyOrThrow(response.body(), VeilederEnheterDTO.class);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbveilederUrl, "/internal/isReady"), client);
    }

}

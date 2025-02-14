package no.nav.veilarbvedtaksstotte.client.arena;

import lombok.Value;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.arena.dto.VeilarbArenaOppfolging;
import no.nav.veilarbvedtaksstotte.client.arena.request.VeilarbarenaOppfolgingRequest;
import no.nav.veilarbvedtaksstotte.config.CacheConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.function.Supplier;

import static no.nav.common.rest.client.RestUtils.parseJsonResponseOrThrow;
import static no.nav.common.rest.client.RestUtils.toJsonRequestBody;
import static no.nav.common.utils.AuthUtils.bearerToken;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.springframework.http.HttpStatus.NOT_FOUND;

public class VeilarbarenaClientImpl implements VeilarbarenaClient {

    private final String veilarbarenaUrl;

    private final OkHttpClient client;

    private final Supplier<String> machineToMachineToken;

    public VeilarbarenaClientImpl(String veilarbarenaUrl, Supplier<String> machineToMachineToken) {
        this.veilarbarenaUrl = veilarbarenaUrl;
        this.client = RestClient.baseClient();
        this.machineToMachineToken = machineToMachineToken;
    }

    @Cacheable(CacheConfig.ARENA_BRUKER_CACHE_NAME)
    public Optional<VeilarbArenaOppfolging> hentOppfolgingsbruker(Fnr fnr) {
        return hentOppfolgingsbrukerIntern(fnr);
    }

    @CachePut(value = CacheConfig.ARENA_BRUKER_CACHE_NAME, key = "#fnr")
    public Optional<VeilarbArenaOppfolging> oppdaterOppfolgingsbruker(Fnr fnr, String navKontor) {
        var oppfolgingsbruker = hentOppfolgingsbrukerIntern(fnr);

        if (oppfolgingsbruker.isEmpty()) {
            return Optional.empty();
        }

        var oppdatertOppfolgingsBruker = new VeilarbArenaOppfolging(navKontor, oppfolgingsbruker.get().getFormidlingsgruppekode(), oppfolgingsbruker.get().getKvalifiseringsgruppekode());
        return Optional.of(oppdatertOppfolgingsBruker);
    }


    private Optional<VeilarbArenaOppfolging> hentOppfolgingsbrukerIntern(Fnr fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "/api/v3/hent-oppfolgingsbruker"))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(machineToMachineToken.get()))
                .post(toJsonRequestBody(new VeilarbarenaOppfolgingRequest(fnr)))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == NOT_FOUND.value()) {
                return Optional.empty();
            }
            RestUtils.throwIfNotSuccessful(response);
            return Optional.ofNullable(parseJsonResponseOrThrow(response, VeilarbArenaOppfolging.class));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot veilarbarena/oppfolgingsbruker");
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbarenaUrl, "/internal/isReady"), client);
    }

    @Override
    public Optional<String> oppfolgingssak(Fnr fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbarenaUrl, "/api/v2/hent-oppfolgingssak"))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(machineToMachineToken.get()))
                .post(toJsonRequestBody(new VeilarbarenaOppfolgingRequest(fnr)))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == NOT_FOUND.value()) {
                return Optional.empty();
            }
            RestUtils.throwIfNotSuccessful(response);
            return Optional.of(parseJsonResponseOrThrow(response, ArenaOppfolgingssak.class).getOppfolgingssakId());
        } catch (Exception e) {
            throw new IllegalStateException("Fant ikke oppfolgingssak i Arena for bruker.", e);
        }
    }

    @Value
    public static class ArenaOppfolgingssak {
        String oppfolgingssakId;
    }
}

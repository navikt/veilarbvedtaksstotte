package no.nav.veilarbvedtaksstotte.client.dokument;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class VeilarbdokumentClientImpl implements VeilarbdokumentClient {

    private final String veilarbdokumentUrl;

    private final OkHttpClient client;

    public VeilarbdokumentClientImpl(String veilarbdokumentUrl) {
        this.veilarbdokumentUrl = veilarbdokumentUrl;
        this.client = RestClient.baseClient();
    }

    @SneakyThrows
    public DokumentSendtDTO sendDokument(SendDokumentDTO sendDokumentDTO) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbdokumentUrl, "/api/bestilldokument"))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .post(RestUtils.toJsonRequestBody(sendDokumentDTO))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, DokumentSendtDTO.class);
        }
    }

    @SneakyThrows
    public byte[] produserDokumentUtkast(SendDokumentDTO sendDokumentDTO) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbdokumentUrl, "/api/dokumentutkast"))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .post(RestUtils.toJsonRequestBody(sendDokumentDTO))
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return response.body().bytes();
        }
    }

    @SneakyThrows
    public byte[] produserDokumentV2(ProduserDokumentV2DTO produserDokumentV2DTO) {

        Request request = new Request.Builder()
                .url(joinPaths(veilarbdokumentUrl, "/api/v2/produserdokument"))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .post(RestUtils.toJsonRequestBody(produserDokumentV2DTO))
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return response.body().bytes();
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(veilarbdokumentUrl, "/internal/health/readiness"), client);
    }

}

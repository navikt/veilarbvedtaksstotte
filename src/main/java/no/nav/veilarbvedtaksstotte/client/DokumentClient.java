package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.domain.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.domain.SendDokumentDTO;
import no.nav.veilarbvedtaksstotte.utils.RestClientUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class DokumentClient {

    private final String veilarbdokumentUrl;

    public DokumentClient(String veilarbdokumentUrl) {
        this.veilarbdokumentUrl = veilarbdokumentUrl;
    }

    @SneakyThrows
    public DokumentSendtDTO sendDokument(SendDokumentDTO sendDokumentDTO) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbdokumentUrl, "/api/bestilldokument"))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .post(RestUtils.toJsonRequestBody(sendDokumentDTO))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestClientUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseBodyOrThrow(response.body(), DokumentSendtDTO.class);
        }
    }

    @SneakyThrows
    public byte[] produserDokumentUtkast(SendDokumentDTO sendDokumentDTO) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarbdokumentUrl, "/api/dokumentutkast"))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .post(RestUtils.toJsonRequestBody(sendDokumentDTO))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestClientUtils.throwIfNotSuccessful(response);
            return response.body().bytes();
        }
    }

}

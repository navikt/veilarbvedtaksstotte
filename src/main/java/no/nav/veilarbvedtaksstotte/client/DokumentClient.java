package no.nav.veilarbvedtaksstotte.client;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.domain.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.domain.SendDokumentDTO;
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
public class DokumentClient {

    public static final String DOKUMENT_API_PROPERTY_NAME = "VEILARBDOKUMENTAPI_URL";
    public static final String VEILARBDOKUMENT = "veilarbdokument";

    private final String veilarbdokumentUrl;

    public DokumentClient() {
        this.veilarbdokumentUrl = getRequiredProperty(DOKUMENT_API_PROPERTY_NAME);
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

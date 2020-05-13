package no.nav.veilarbvedtaksstotte.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbvedtaksstotte.domain.Journalpost;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;

import java.util.Arrays;
import java.util.List;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.veilarbvedtaksstotte.utils.RestClientUtils.authHeaderMedInnloggetBruker;

public class SafClient implements HealthCheck {

    private static final Gson gson = new Gson();

    private final String safUrl;

    private final OkHttpClient client;

    public SafClient(String safUrl) {
        this.safUrl = safUrl;
        this.client = RestClient.baseClient();
    }

    @SneakyThrows
    public byte[] hentVedtakPdf(String journalpostId, String dokumentInfoId) {
        Request request = new Request.Builder()
                .url(joinPaths(safUrl, "/rest/hentdokument/", journalpostId, dokumentInfoId, "ARKIV"))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return response.body().bytes();
        }
    }

    @SneakyThrows
    public List<Journalpost> hentJournalposter(String fnr) {
        GraphqlRequest graphqlRequest = new GraphqlRequest(createDokumentoversiktBrukerGqlStr(fnr));

        Request request = new Request.Builder()
                .url(joinPaths(safUrl, "graphql"))
                .header(HttpHeaders.AUTHORIZATION, authHeaderMedInnloggetBruker())
                .post(RestUtils.toJsonRequestBody(graphqlRequest))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            String json = response.body().string();
            return Arrays.asList(hentJournalposterFraJson(json));
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(safUrl, "/isReady"), client);
    }

    private Journalpost[] hentJournalposterFraJson(String journalposterGraphqlJsonData) {
        JsonElement journalPosterElement = JsonParser.parseString(journalposterGraphqlJsonData);

        String journalposterJson = journalPosterElement
                .getAsJsonObject()
                .getAsJsonObject("data")
                .getAsJsonObject("dokumentoversiktBruker")
                .getAsJsonArray("journalposter")
                .toString();

        return gson.fromJson(journalposterJson, Journalpost[].class);
    }

    private String createDokumentoversiktBrukerGqlStr(String fnr) {
        return String.format("{\n" +
                "  dokumentoversiktBruker(brukerId: {id: \"%s\", type: FNR}, foerste: 50, tema: OPP) {\n" +
                "    journalposter {\n" +
                "      journalpostId\n" +
                "      tittel\n" +
                "      dokumenter {\n" +
                "        dokumentInfoId\n" +
                "        datoFerdigstilt\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", fnr);
    }

    private static class GraphqlRequest {
        public String query;

        public Object variables;

        public GraphqlRequest(String query) {
            this.query = query;
        }
    }
}

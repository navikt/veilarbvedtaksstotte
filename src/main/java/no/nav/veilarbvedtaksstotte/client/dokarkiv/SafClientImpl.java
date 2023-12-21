package no.nav.veilarbvedtaksstotte.client.dokarkiv;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.utils.graphql.GraphqlRequest;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.Journalpost;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.JournalpostGraphqlResponse;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.BrukerId;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.BrukerIdType;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.DokumentOversiktBrukerVariables;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.QueryVariables;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static no.nav.common.utils.UrlUtils.joinPaths;

@Slf4j
public class SafClientImpl implements SafClient {

    private static final Gson gson = new Gson();

    private final String safUrl;

    private final OkHttpClient client;

    private final Supplier<String> userTokenSupplier;

    public SafClientImpl(String safUrl, Supplier<String> userTokenSupplier) {
        this.safUrl = safUrl;
        this.client = RestClient.baseClient();
        this.userTokenSupplier = userTokenSupplier;
    }

    @SneakyThrows
    public byte[] hentVedtakPdf(String journalpostId, String dokumentInfoId) {
        Request request = new Request.Builder()
                .url(joinPaths(safUrl, "/rest/hentdokument/", journalpostId, dokumentInfoId, "ARKIV"))
                .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return response.body().bytes();
        }
    }

    @SneakyThrows
    public List<Journalpost> hentJournalposter(Fnr fnr) {
        String journalpostOppfolgingTema = "OPP";
        int journalpostMaxDokumenter = 50;
        GraphqlRequest<DokumentOversiktBrukerVariables> graphqlRequest = new GraphqlRequest<>(createDokumentoversiktBrukerGqlStr(), new DokumentOversiktBrukerVariables(new BrukerId(fnr.get(), BrukerIdType.FNR), journalpostOppfolgingTema, journalpostMaxDokumenter));

        Request request = new Request.Builder()
                .url(joinPaths(safUrl, "graphql"))
                .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
                .post(RestUtils.toJsonRequestBody(graphqlRequest))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            String json = response.body().string();
            return Arrays.asList(hentJournalposterFraJson(json));
        }
    }

    @SneakyThrows
    public JournalpostGraphqlResponse hentJournalpost(String journalpostId) {
        GraphqlRequest<QueryVariables> graphqlRequest = new GraphqlRequest<>(createJournalpostGqlStr(), new QueryVariables(journalpostId));

        Request request = new Request.Builder()
                .url(joinPaths(safUrl, "graphql"))
                .header(HttpHeaders.AUTHORIZATION, userTokenSupplier.get())
                .post(RestUtils.toJsonRequestBody(graphqlRequest))
                .build();

        try (Response response = RestClient.baseClient().newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, JournalpostGraphqlResponse.class);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(safUrl, "/actuator/health/readiness"), client);
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

    private String createDokumentoversiktBrukerGqlStr() {
        return """
                query(
                    $brukerId: BrukerIdInput!, $foerste: Int!, $tema: [Tema])
                {
                  dokumentoversiktBruker(brukerId: $brukerId, foerste: $foerste, tema: $tema) {
                    journalposter {
                      journalpostId
                      tittel
                      dokumenter {
                        dokumentInfoId
                        datoFerdigstilt
                      }
                    }
                  }
                }
                """;
    }

    private String createJournalpostGqlStr() {
        return """
                query journalpost($journalPostId: String!)  {
                    journalpost(journalpostId: $journalPostId) {
                        dokumenter {
                            dokumentInfoId
                            tittel
                            brevkode
                            dokumentstatus
                        }
                    }
                }
                """;
    }

}

package no.nav.veilarbvedtaksstotte.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.Journalpost;
import no.nav.json.JsonProvider;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class SAFClient extends BaseClient {

    private static final ObjectMapper objectMapper = JsonProvider.createObjectMapper();
    
    public static final String SAF_API_PROPERTY_NAME = "SAF_HENTDOKUMENT_URL";
    public static final String SAF = "saf";

    public SAFClient(String apiUrl) {
        super(apiUrl);
    }

    @Inject
    public SAFClient() {
        super(getRequiredProperty(SAF_API_PROPERTY_NAME));
    }

    public byte[] hentVedtakPdf(String journalpostId, String dokumentInfoId) {
        return get(joinPaths(baseUrl, "rest", "hentdokument", journalpostId, dokumentInfoId, "ARKIV"), byte[].class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot saf/hentdokument"));
    }

    @SneakyThrows(JsonProcessingException.class)
    public List<Journalpost> hentJournalposter(String fnr) {
        GraphqlRequest graphqlRequest = new GraphqlRequest(createDokumentoversiktBrukerGqlStr(fnr));

        String arkiverteVedtakJson = post(joinPaths(baseUrl, "graphql"), graphqlRequest, String.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot saf/graphql"));

        return Arrays.asList(hentJournalposterFraJson(arkiverteVedtakJson));
    }

    private Journalpost[] hentJournalposterFraJson(String journalposterGraphqlJsonData) throws JsonProcessingException {
        JsonNode journalPosterNode = objectMapper.readTree(journalposterGraphqlJsonData);

        String journalposterJson = journalPosterNode
                .get("data")
                .get("dokumentoversiktBruker")
                .get("journalposter")
                .toString();

        return objectMapper.readValue(journalposterJson, Journalpost[].class);
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
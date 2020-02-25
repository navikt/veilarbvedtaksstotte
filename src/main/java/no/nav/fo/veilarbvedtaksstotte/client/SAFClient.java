package no.nav.fo.veilarbvedtaksstotte.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.Journalpost;
import no.nav.json.JsonProvider;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class SAFClient extends BaseClient {

    private static final ObjectMapper objectMapper = JsonProvider.createObjectMapper();
    
    public static final String SAF_API_PROPERTY_NAME = "SAF_HENTDOKUMENT_URL";
    public static final String SAF = "saf";

    @Inject
    public SAFClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        super(getRequiredProperty(SAF_API_PROPERTY_NAME), httpServletRequestProvider);
    }

    public byte[] hentVedtakPdf(String journalpostId, String dokumentInfoId) {
        return get(joinPaths(baseUrl, "rest", "hentdokument", journalpostId, dokumentInfoId, "ARKIV"), byte[].class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot saf/hentdokument"));
    }

    @SneakyThrows(JsonProcessingException.class)
    public List<Journalpost> hentJournalposter(String fnr) {
        String gqlQuery = createDokumentoversiktBrukerGqlStr(fnr);

        String arkiverteVedtakJson = post(joinPaths(baseUrl, "graphql"), gqlQuery, String.class)
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
                "      journalforendeEnhet\n" +
                "      journalfortAvNavn\n" +
                "      datoOpprettet\n" +
                "      dokumenter {\n" +
                "        dokumentInfoId\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", fnr);
    }
}

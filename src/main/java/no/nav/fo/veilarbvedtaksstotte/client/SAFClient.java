package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class SAFClient extends BaseClient {

    public static final String SAF_API_PROPERTY_NAME = "SAF_HENTDOKUMENT_URL";
    public static final String SAF = "saf";

    @Inject
    public SAFClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        super(getRequiredProperty(SAF_API_PROPERTY_NAME), httpServletRequestProvider);
    }

    public byte[] hentVedtakPdf(String journalpostId, String dokumentInfoId) {
        return get(joinPaths(baseUrl, "rest", "hentdokument", journalpostId,dokumentInfoId, "ARKIV"), byte[].class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot saf/hentdokument"));
    }
}

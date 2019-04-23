package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.DokumentSendtDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.SendDokumentDTO;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class DokumentClient extends BaseClient {

    public static final String DOKUMENT_API_PROPERTY_NAME = "VEILARBDOKUMENTAPI_URL";
    public static final String VEILARBDOKUMENT = "veilarbdokument";

    @Inject
    public DokumentClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        super(getRequiredProperty(DOKUMENT_API_PROPERTY_NAME), httpServletRequestProvider);
    }

    public DokumentSendtDTO sendDokument(SendDokumentDTO sendDokumentDTO) {
        return post(joinPaths(baseUrl, "api", "bestilldokument"), sendDokumentDTO, DokumentSendtDTO.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot veilarbdokument/bestilldokument"));
    }

}

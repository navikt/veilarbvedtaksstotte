package no.nav.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.domain.SendDokumentDTO;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class DokumentClient extends BaseClient {

    public static final String DOKUMENT_API_PROPERTY_NAME = "VEILARBDOKUMENTAPI_URL";
    public static final String VEILARBDOKUMENT = "veilarbdokument";

    public DokumentClient() {
        super(getRequiredProperty(DOKUMENT_API_PROPERTY_NAME));
    }

    public DokumentSendtDTO sendDokument(SendDokumentDTO sendDokumentDTO) {
        return post(joinPaths(baseUrl, "api", "bestilldokument"), sendDokumentDTO, DokumentSendtDTO.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot veilarbdokument/bestilldokument"));
    }

    public byte[] produserDokumentUtkast(SendDokumentDTO sendDokumentDTO) {
        return post(joinPaths(baseUrl, "api", "dokumentutkast"), sendDokumentDTO,  byte[].class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot veilarbdokument/dokumentutkast"));
    }

}

import no.nav.apiapp.ApiApp;
import no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig;

import static java.lang.System.setProperty;
import static no.nav.apiapp.util.UrlUtils.clusterUrlForApplication;
import static no.nav.dialogarena.aktor.AktorConfig.AKTOER_ENDPOINT_URL;
import static no.nav.fo.veilarbvedtaksstotte.client.DokumentClient.DOKUMENT_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.DokumentClient.VEILARBDOKUMENT;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class Main {

    public static void main(String... args) {
        setProperty(AKTOER_ENDPOINT_URL, getRequiredProperty("AKTOER_V2_ENDPOINTURL"));
        setProperty(DOKUMENT_API_PROPERTY_NAME, clusterUrlForApplication(VEILARBDOKUMENT));
        ApiApp.startApiApp(ApplicationConfig.class, args);
    }

}

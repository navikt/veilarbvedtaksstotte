import no.nav.apiapp.ApiApp;
import no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig;

import static java.lang.System.setProperty;
import static no.nav.dialogarena.aktor.AktorConfig.AKTOER_ENDPOINT_URL;
import static no.nav.fo.veilarbvedtaksstotte.client.CVClient.PAM_CV_API;
import static no.nav.fo.veilarbvedtaksstotte.client.CVClient.CV_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.DokumentClient.DOKUMENT_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.DokumentClient.VEILARBDOKUMENT;
import static no.nav.fo.veilarbvedtaksstotte.client.ArenaClient.VEILARBARENA_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.ArenaClient.VEILARBARENA;
import static no.nav.fo.veilarbvedtaksstotte.client.EgenvurderingClient.VEILARBVEDTAKINFO;
import static no.nav.fo.veilarbvedtaksstotte.client.EgenvurderingClient.EGENVURDERING_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.PersonClient.PERSON_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.PersonClient.VEILARBPERSON;
import static no.nav.fo.veilarbvedtaksstotte.client.RegistreringClient.VEILARBREGISTRERING;
import static no.nav.fo.veilarbvedtaksstotte.client.RegistreringClient.REGISTRERING_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.SAFClient.SAF;
import static no.nav.fo.veilarbvedtaksstotte.client.SAFClient.SAF_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.utils.UrlUtils.lagClusterUrl;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class Main {

    public static void main(String... args) {
        setProperty(AKTOER_ENDPOINT_URL, getRequiredProperty("AKTOER_V2_ENDPOINTURL"));
        setProperty(DOKUMENT_API_PROPERTY_NAME, lagClusterUrl(VEILARBDOKUMENT));
        setProperty(PERSON_API_PROPERTY_NAME, lagClusterUrl(VEILARBPERSON));
        setProperty(CV_API_PROPERTY_NAME, lagClusterUrl(PAM_CV_API));
        setProperty(REGISTRERING_API_PROPERTY_NAME, lagClusterUrl(VEILARBREGISTRERING));
        setProperty(EGENVURDERING_API_PROPERTY_NAME, lagClusterUrl(VEILARBVEDTAKINFO));
        setProperty(VEILARBARENA_API_PROPERTY_NAME, lagClusterUrl(VEILARBARENA));
        setProperty(SAF_API_PROPERTY_NAME, lagClusterUrl(SAF,false));
        ApiApp.startApiApp(ApplicationConfig.class, args);
    }

}

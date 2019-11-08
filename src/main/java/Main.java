import no.nav.apiapp.ApiApp;
import no.nav.brukerdialog.tools.SecurityConstants;
import no.nav.common.utils.NaisUtils;
import no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.sbl.util.EnvironmentUtils;

import static java.lang.System.setProperty;
import static no.nav.common.utils.NaisUtils.getCredentials;
import static no.nav.dialogarena.aktor.AktorConfig.AKTOER_ENDPOINT_URL;
import static no.nav.fo.veilarbvedtaksstotte.client.CVClient.PAM_CV_API;
import static no.nav.fo.veilarbvedtaksstotte.client.CVClient.CV_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.DokumentClient.DOKUMENT_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.DokumentClient.VEILARBDOKUMENT;
import static no.nav.fo.veilarbvedtaksstotte.client.ArenaClient.VEILARBARENA_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.ArenaClient.VEILARBARENA;
import static no.nav.fo.veilarbvedtaksstotte.client.EgenvurderingClient.VEILARBVEDTAKINFO;
import static no.nav.fo.veilarbvedtaksstotte.client.EgenvurderingClient.EGENVURDERING_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.OppgaveClient.OPPGAVE;
import static no.nav.fo.veilarbvedtaksstotte.client.OppgaveClient.OPPGAVE_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.VeiledereOgEnhetClient.VEILARBVEILEDER;
import static no.nav.fo.veilarbvedtaksstotte.client.VeiledereOgEnhetClient.VEILARBVEILEDER_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.OppfolgingClient.VEILARBOPPFOLGING;
import static no.nav.fo.veilarbvedtaksstotte.client.OppfolgingClient.VEILARBOPPFOLGING_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.PersonClient.PERSON_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.PersonClient.VEILARBPERSON;
import static no.nav.fo.veilarbvedtaksstotte.client.RegistreringClient.VEILARBREGISTRERING;
import static no.nav.fo.veilarbvedtaksstotte.client.RegistreringClient.REGISTRERING_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.SAFClient.SAF;
import static no.nav.fo.veilarbvedtaksstotte.client.SAFClient.SAF_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.SECURITYTOKENSERVICE_URL;
import static no.nav.fo.veilarbvedtaksstotte.config.DatabaseConfig.VEILARBVEDTAKSSTOTTE_DB_PASSWORD;
import static no.nav.fo.veilarbvedtaksstotte.config.DatabaseConfig.VEILARBVEDTAKSSTOTTE_DB_USERNAME;
import static no.nav.fo.veilarbvedtaksstotte.utils.UrlUtils.lagClusterUrl;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class Main {

    public static void main(String... args) {
        readFromConfigMap();

        NaisUtils.Credentials serviceUser = getCredentials("service_user");

        //STS
        setProperty(StsSecurityConstants.STS_URL_KEY, getRequiredProperty(SECURITYTOKENSERVICE_URL));
        setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        //ABAC
        System.setProperty(CredentialConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        //OIDC
        System.setProperty(SecurityConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(SecurityConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        setProperty(AKTOER_ENDPOINT_URL, getRequiredProperty("AKTOER_V2_ENDPOINTURL"));
        setProperty(DOKUMENT_API_PROPERTY_NAME, lagClusterUrl(VEILARBDOKUMENT));
        setProperty(PERSON_API_PROPERTY_NAME, lagClusterUrl(VEILARBPERSON));
        setProperty(VEILARBVEILEDER_API_PROPERTY_NAME, lagClusterUrl(VEILARBVEILEDER));
        setProperty(REGISTRERING_API_PROPERTY_NAME, lagClusterUrl(VEILARBREGISTRERING));
        setProperty(EGENVURDERING_API_PROPERTY_NAME, lagClusterUrl(VEILARBVEDTAKINFO));
        setProperty(VEILARBARENA_API_PROPERTY_NAME, lagClusterUrl(VEILARBARENA));
        setProperty(SAF_API_PROPERTY_NAME, lagClusterUrl(SAF,false));
        setProperty(VEILARBOPPFOLGING_API_PROPERTY_NAME, lagClusterUrl(VEILARBOPPFOLGING));
        setProperty(OPPGAVE_API_PROPERTY_NAME, lagClusterUrl(OPPGAVE, false));

        // PAM CV finnes ikke i Q1, gå mot Q6 istedenfor
        if (EnvironmentUtils.requireEnvironmentName().equalsIgnoreCase("q1")) {
            String pamCVUrl = lagClusterUrl(PAM_CV_API).replace("q1", "q6");
            setProperty(CV_API_PROPERTY_NAME, pamCVUrl);
        } else {
            setProperty(CV_API_PROPERTY_NAME, lagClusterUrl(PAM_CV_API));
        }

        NaisUtils.Credentials oracleCreds = getCredentials("oracle_creds");
        System.setProperty(VEILARBVEDTAKSSTOTTE_DB_USERNAME, oracleCreds.username);
        System.setProperty(VEILARBVEDTAKSSTOTTE_DB_PASSWORD, oracleCreds.password);

        ApiApp.startApiApp(ApplicationConfig.class, args);
    }

    private static void readFromConfigMap() {
        NaisUtils.addConfigMapToEnv("pto-config",
                "KAFKA_BROKERS_URL",
                "AKTOER_V2_SECURITYTOKEN",
                "AKTOER_V2_ENDPOINTURL",
                "AKTOER_V2_WSDLURL",
                "ABAC_PDP_ENDPOINT_URL",
                "ABAC_PDP_ENDPOINT_DESCRIPTION",
                "ISSO_HOST_URL",
                "ISSO_JWKS_URL",
                "ISSO_ISSUER_URL",
                "ISSO_ISALIVE_URL",
                "SECURITYTOKENSERVICE_URL",
                "UNLEASH_API_URL",
                "VEILARBLOGIN_REDIRECT_URL_DESCRIPTION",
                "OIDC_REDIRECT_URL"
        );
    }

}

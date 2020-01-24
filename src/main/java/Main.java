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
import static no.nav.fo.veilarbvedtaksstotte.config.PepConfig.VEILARBABAC;
import static no.nav.fo.veilarbvedtaksstotte.config.PepConfig.VEILARBABAC_API_URL_PROPERTY;
import static no.nav.fo.veilarbvedtaksstotte.utils.UrlUtils.lagClusterUrl;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class Main {

    public static void main(String... args) {
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
        setProperty(VEILARBABAC_API_URL_PROPERTY, lagClusterUrl(VEILARBABAC, false));
        setProperty(VEILARBVEILEDER_API_PROPERTY_NAME, lagClusterUrl(VEILARBVEILEDER));
        setProperty(REGISTRERING_API_PROPERTY_NAME, lagClusterUrl(VEILARBREGISTRERING));
        setProperty(EGENVURDERING_API_PROPERTY_NAME, lagClusterUrl(VEILARBVEDTAKINFO));
        setProperty(VEILARBARENA_API_PROPERTY_NAME, lagClusterUrl(VEILARBARENA));
        setProperty(SAF_API_PROPERTY_NAME, lagClusterUrl(SAF,false));
        setProperty(VEILARBOPPFOLGING_API_PROPERTY_NAME, lagClusterUrl(VEILARBOPPFOLGING));

        // PAM CV finnes ikke i Q1, gå mot Q0 istedenfor
        // Oppgave i Q1 ligger på default namespace
        if (EnvironmentUtils.requireEnvironmentName().equalsIgnoreCase("q1")) {
            String pamCVUrl = lagClusterUrl(PAM_CV_API).replace("q1", "q0");
            setProperty(CV_API_PROPERTY_NAME, pamCVUrl);
            setProperty(OPPGAVE_API_PROPERTY_NAME, "https://oppgave.nais.preprod.local");
        } else {
            setProperty(CV_API_PROPERTY_NAME, lagClusterUrl(PAM_CV_API));
            setProperty(OPPGAVE_API_PROPERTY_NAME, lagClusterUrl(OPPGAVE, false));
        }

        ApiApp.startApiApp(ApplicationConfig.class, args);
    }

}

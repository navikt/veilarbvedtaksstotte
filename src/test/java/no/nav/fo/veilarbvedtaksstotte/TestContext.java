package no.nav.fo.veilarbvedtaksstotte;

import no.nav.brukerdialog.security.Constants;
import no.nav.brukerdialog.tools.SecurityConstants;
import no.nav.fasit.FasitUtils;
import no.nav.fasit.ServiceUser;
import no.nav.fasit.WebServiceEndpoint;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;

import static java.lang.System.setProperty;
import static no.nav.dialogarena.aktor.AktorConfig.AKTOER_ENDPOINT_URL;
import static no.nav.fasit.FasitUtils.Zone.FSS;
import static no.nav.fasit.FasitUtils.*;
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
import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.fo.veilarbvedtaksstotte.config.KafkaProperties.KAFKA_BROKERS_URL_PROPERTY;
import static no.nav.fo.veilarbvedtaksstotte.config.PepConfig.VEILARBABAC;
import static no.nav.fo.veilarbvedtaksstotte.config.PepConfig.VEILARBABAC_API_URL_PROPERTY;
import static no.nav.fo.veilarbvedtaksstotte.utils.TestUtils.lagFssUrl;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;


public class TestContext {

    public static void setup() {
        String securityTokenService = FasitUtils.getBaseUrl("securityTokenService", FSS);
        ServiceUser srvVeilarbvedtaksstotte = getServiceUser("srvveilarbvedtaksstotte", APPLICATION_NAME);

        setProperty("APP_NAME", APPLICATION_NAME);
        setProperty(DOKUMENT_API_PROPERTY_NAME, lagFssUrl(VEILARBDOKUMENT));
        setProperty(SAF_API_PROPERTY_NAME, lagFssUrl(SAF,false));
        setProperty(PERSON_API_PROPERTY_NAME, lagFssUrl(VEILARBPERSON));

        setProperty(CV_API_PROPERTY_NAME, lagFssUrl(PAM_CV_API));
        setProperty(REGISTRERING_API_PROPERTY_NAME, lagFssUrl(VEILARBREGISTRERING));
        setProperty(EGENVURDERING_API_PROPERTY_NAME, lagFssUrl(VEILARBVEDTAKINFO));
        setProperty(VEILARBARENA_API_PROPERTY_NAME, lagFssUrl(VEILARBARENA));
        setProperty(VEILARBABAC_API_URL_PROPERTY, lagFssUrl(VEILARBABAC, false));
        setProperty(KAFKA_BROKERS_URL_PROPERTY, "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443");

        //sts
        setProperty(StsSecurityConstants.STS_URL_KEY, securityTokenService);
        setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, srvVeilarbvedtaksstotte.getUsername());
        setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, srvVeilarbvedtaksstotte.getPassword());

        // Abac
        setProperty(CredentialConstants.SYSTEMUSER_USERNAME, srvVeilarbvedtaksstotte.getUsername());
        setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, srvVeilarbvedtaksstotte.getPassword());
        setProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME, "https://wasapp-" + getDefaultEnvironment() + ".adeo.no/asm-pdp/authorize");

        WebServiceEndpoint aktorEndpoint = getWebServiceEndpoint("Aktoer_v2", getDefaultEnvironment());
        setProperty(AKTOER_ENDPOINT_URL, aktorEndpoint.getUrl());

        String issoHost = FasitUtils.getBaseUrl("isso-host");
        String issoJWS = FasitUtils.getBaseUrl("isso-jwks");
        String issoISSUER = FasitUtils.getBaseUrl("isso-issuer");
        String issoIsAlive = FasitUtils.getBaseUrl("isso.isalive", FSS);
        ServiceUser isso_rp_user = getServiceUser("isso-rp-user", APPLICATION_NAME);
        String loginUrl = getRestService("veilarblogin.redirect-url", getDefaultEnvironment(), "fss").getUrl();

        setProperty(Constants.ISSO_HOST_URL_PROPERTY_NAME, issoHost);
        setProperty(Constants.ISSO_RP_USER_USERNAME_PROPERTY_NAME, isso_rp_user.getUsername());
        setProperty(Constants.ISSO_RP_USER_PASSWORD_PROPERTY_NAME, isso_rp_user.getPassword());
        setProperty(Constants.ISSO_JWKS_URL_PROPERTY_NAME, issoJWS);
        setProperty(Constants.ISSO_ISSUER_URL_PROPERTY_NAME, issoISSUER);
        setProperty(Constants.ISSO_ISALIVE_URL_PROPERTY_NAME, issoIsAlive);
        setProperty(Constants.OIDC_REDIRECT_URL_PROPERTY_NAME, loginUrl);
        setProperty(SecurityConstants.SYSTEMUSER_USERNAME, srvVeilarbvedtaksstotte.getUsername());
        setProperty(SecurityConstants.SYSTEMUSER_PASSWORD, srvVeilarbvedtaksstotte.getPassword());

    }
}

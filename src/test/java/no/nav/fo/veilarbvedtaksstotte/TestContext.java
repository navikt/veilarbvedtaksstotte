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
import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;


public class TestContext {

    public static void setup() {
        String securityTokenService = FasitUtils.getBaseUrl("securityTokenService", FSS);
        ServiceUser srvVeilarbvedtaksstotte = getServiceUser("srvveilarbvedtaksstotte", APPLICATION_NAME);

        setProperty("APP_NAME", APPLICATION_NAME);

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
        String loginUrl = getRestService("veilarblogin.redirect-url", getDefaultEnvironment()).getUrl();

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

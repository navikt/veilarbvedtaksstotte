package no.nav.veilarbvedtaksstotte;

import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;

import static java.lang.System.setProperty;
import static no.nav.veilarbvedtaksstotte.client.CVClient.PAM_CV_API;
import static no.nav.veilarbvedtaksstotte.client.CVClient.CV_API_PROPERTY_NAME;
import static no.nav.veilarbvedtaksstotte.client.DokumentClient.DOKUMENT_API_PROPERTY_NAME;
import static no.nav.veilarbvedtaksstotte.client.DokumentClient.VEILARBDOKUMENT;
import static no.nav.veilarbvedtaksstotte.client.ArenaClient.VEILARBARENA_API_PROPERTY_NAME;
import static no.nav.veilarbvedtaksstotte.client.ArenaClient.VEILARBARENA;
import static no.nav.veilarbvedtaksstotte.client.EgenvurderingClient.VEILARBVEDTAKINFO;
import static no.nav.veilarbvedtaksstotte.client.EgenvurderingClient.EGENVURDERING_API_PROPERTY_NAME;
import static no.nav.veilarbvedtaksstotte.client.VeiledereOgEnhetClient.VEILARBVEILEDER;
import static no.nav.veilarbvedtaksstotte.client.VeiledereOgEnhetClient.VEILARBVEILEDER_API_PROPERTY_NAME;
import static no.nav.veilarbvedtaksstotte.client.OppfolgingClient.VEILARBOPPFOLGING;
import static no.nav.veilarbvedtaksstotte.client.OppfolgingClient.VEILARBOPPFOLGING_API_PROPERTY_NAME;
import static no.nav.veilarbvedtaksstotte.client.OppgaveClient.OPPGAVE;
import static no.nav.veilarbvedtaksstotte.client.OppgaveClient.OPPGAVE_API_PROPERTY_NAME;
import static no.nav.veilarbvedtaksstotte.client.RegistreringClient.VEILARBREGISTRERING;
import static no.nav.veilarbvedtaksstotte.client.RegistreringClient.REGISTRERING_API_PROPERTY_NAME;
import static no.nav.veilarbvedtaksstotte.client.SAFClient.SAF;
import static no.nav.veilarbvedtaksstotte.client.SAFClient.SAF_API_PROPERTY_NAME;
import static no.nav.veilarbvedtaksstotte.config.ApplicationConfig.*;
import static no.nav.veilarbvedtaksstotte.config.DatabaseConfig.VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY;
import static no.nav.veilarbvedtaksstotte.utils.TestUtils.lagFssUrl;
import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.UNLEASH_API_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.*;

public class TestContext {

    public static void setup() {
        setProperty(APP_NAME_PROPERTY_NAME, APPLICATION_NAME);
        setProperty(APP_ENVIRONMENT_NAME_PROPERTY_NAME, "q0");
        setProperty(NAIS_NAMESPACE_PROPERTY_NAME, "q0");

        setProperty(DOKUMENT_API_PROPERTY_NAME, TestUtils.lagFssUrl(VEILARBDOKUMENT));
        setProperty(SAF_API_PROPERTY_NAME, TestUtils.lagFssUrl(SAF,false));
        setProperty(VEILARBVEILEDER_API_PROPERTY_NAME, TestUtils.lagFssUrl(VEILARBVEILEDER));
        setProperty(VEILARBOPPFOLGING_API_PROPERTY_NAME, TestUtils.lagFssUrl(VEILARBOPPFOLGING));
        setProperty(OPPGAVE_API_PROPERTY_NAME, TestUtils.lagFssUrl(OPPGAVE, false));

        setProperty(CV_API_PROPERTY_NAME, TestUtils.lagFssUrl(PAM_CV_API));
        setProperty(REGISTRERING_API_PROPERTY_NAME, TestUtils.lagFssUrl(VEILARBREGISTRERING));
        setProperty(EGENVURDERING_API_PROPERTY_NAME, TestUtils.lagFssUrl(VEILARBVEDTAKINFO));
        setProperty(VEILARBARENA_API_PROPERTY_NAME, TestUtils.lagFssUrl(VEILARBARENA));
        setProperty(KAFKA_BROKERS_URL_PROPERTY, "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443");
        setProperty(UNLEASH_API_URL_PROPERTY_NAME, "https://unleash.nais.adeo.no/api/");

        setProperty(SECURITYTOKENSERVICE_URL, "DUMMY_URL");
        setProperty("SRVVEILARBVEDTAKSSTOTTE_USERNAME", "DUMMY_USERNAME");
        setProperty("SRVVEILARBVEDTAKSSTOTTE_PASSWORD", "DUMMY_USERNAME");

        setProperty("ABAC_PDP_ENDPOINT_URL", "DUMMY_URL");

        setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, "DUMMY_USERNAME");
        setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, "DUMMY_PASSWORD");

        setProperty("ISSO_HOST_URL", "DUMMY_URL");
        setProperty("ISSO_RP_USER_USERNAME", "DUMMY_USERNAME");
        setProperty("ISSO_RP_USER_PASSWORD", "DUMMY_PASSWORD");
        setProperty("ISSO_JWKS_URL", "DUMMY_URL");
        setProperty("ISSO_ISSUER_URL", "DUMMY_URL");
        setProperty("OIDC_REDIRECT_URL", "DUMMY_URL");

        setProperty(VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY, "jdbc:postgresql://localhost:5432/veilarbvedtaksstotte");
    }
}

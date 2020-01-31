package no.nav.fo.veilarbvedtaksstotte;

import static java.lang.System.setProperty;
import static no.nav.fo.veilarbvedtaksstotte.client.CVClient.PAM_CV_API;
import static no.nav.fo.veilarbvedtaksstotte.client.CVClient.CV_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.DokumentClient.DOKUMENT_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.DokumentClient.VEILARBDOKUMENT;
import static no.nav.fo.veilarbvedtaksstotte.client.ArenaClient.VEILARBARENA_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.ArenaClient.VEILARBARENA;
import static no.nav.fo.veilarbvedtaksstotte.client.EgenvurderingClient.VEILARBVEDTAKINFO;
import static no.nav.fo.veilarbvedtaksstotte.client.EgenvurderingClient.EGENVURDERING_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.VeiledereOgEnhetClient.VEILARBVEILEDER;
import static no.nav.fo.veilarbvedtaksstotte.client.VeiledereOgEnhetClient.VEILARBVEILEDER_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.OppfolgingClient.VEILARBOPPFOLGING;
import static no.nav.fo.veilarbvedtaksstotte.client.OppfolgingClient.VEILARBOPPFOLGING_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.OppgaveClient.OPPGAVE;
import static no.nav.fo.veilarbvedtaksstotte.client.OppgaveClient.OPPGAVE_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.RegistreringClient.VEILARBREGISTRERING;
import static no.nav.fo.veilarbvedtaksstotte.client.RegistreringClient.REGISTRERING_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.client.SAFClient.SAF;
import static no.nav.fo.veilarbvedtaksstotte.client.SAFClient.SAF_API_PROPERTY_NAME;
import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.*;
import static no.nav.fo.veilarbvedtaksstotte.config.DatabaseConfig.VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY;
import static no.nav.fo.veilarbvedtaksstotte.config.PepConfig.VEILARBABAC;
import static no.nav.fo.veilarbvedtaksstotte.config.PepConfig.VEILARBABAC_API_URL_PROPERTY;
import static no.nav.fo.veilarbvedtaksstotte.utils.TestUtils.lagFssUrl;
import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.UNLEASH_API_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.*;

public class TestContext {

    public static void setup() {
        setProperty(APP_NAME_PROPERTY_NAME, APPLICATION_NAME);
        setProperty(APP_ENVIRONMENT_NAME_PROPERTY_NAME, "q0");
        setProperty(NAIS_NAMESPACE_PROPERTY_NAME, "q0");

        setProperty(DOKUMENT_API_PROPERTY_NAME, lagFssUrl(VEILARBDOKUMENT));
        setProperty(SAF_API_PROPERTY_NAME, lagFssUrl(SAF,false));
        setProperty(VEILARBVEILEDER_API_PROPERTY_NAME, lagFssUrl(VEILARBVEILEDER));
        setProperty(VEILARBOPPFOLGING_API_PROPERTY_NAME, lagFssUrl(VEILARBOPPFOLGING));
        setProperty(OPPGAVE_API_PROPERTY_NAME, lagFssUrl(OPPGAVE, false));

        setProperty(CV_API_PROPERTY_NAME, lagFssUrl(PAM_CV_API));
        setProperty(REGISTRERING_API_PROPERTY_NAME, lagFssUrl(VEILARBREGISTRERING));
        setProperty(EGENVURDERING_API_PROPERTY_NAME, lagFssUrl(VEILARBVEDTAKINFO));
        setProperty(VEILARBARENA_API_PROPERTY_NAME, lagFssUrl(VEILARBARENA));
        setProperty(VEILARBABAC_API_URL_PROPERTY, lagFssUrl(VEILARBABAC, false));
        setProperty(KAFKA_BROKERS_URL_PROPERTY, "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443");
        setProperty(UNLEASH_API_URL_PROPERTY_NAME, "https://unleash.nais.adeo.no/api/");

        setProperty(SECURITYTOKENSERVICE_URL, "DUMMY_URL");
        setProperty("SRVVEILARBVEDTAKSSTOTTE_USERNAME", "DUMMY_USERNAME");
        setProperty("SRVVEILARBVEDTAKSSTOTTE_PASSWORD", "DUMMY_PASSWORD");

        setProperty(VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY, "jdbc:postgresql://localhost:5432/veilarbvedtaksstotte");
    }
}

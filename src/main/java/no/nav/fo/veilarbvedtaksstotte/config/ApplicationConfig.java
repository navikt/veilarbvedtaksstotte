package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.ServletUtil;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.oidc.auth.OidcAuthenticatorConfig;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.fo.veilarbvedtaksstotte.utils.DbRole;
import no.nav.fo.veilarbvedtaksstotte.utils.DbUtils;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletContext;

import static no.nav.brukerdialog.security.Constants.ID_TOKEN_COOKIE_NAME;
import static no.nav.brukerdialog.security.Constants.REFRESH_TOKEN_COOKIE_NAME;
import static no.nav.fo.veilarbvedtaksstotte.config.DatabaseConfig.VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;


@Configuration
@EnableScheduling
@Import({
        ResourceConfig.class,
        DatabaseConfig.class,
        PepConfig.class,
        AktorConfig.class,
        KafkaProducerConfig.class,
        KafkaConsumerConfig.class,
        ServiceConfig.class,
        ClientConfig.class,
        CacheConfig.class,
        RepositoryConfig.class,
        ScheduleConfig.class,
        FeatureToggleConfig.class
})
public class ApplicationConfig implements ApiApplication {

    public static final String APPLICATION_NAME = "veilarbvedtaksstotte";
    public static final String KAFKA_BROKERS_URL_PROPERTY = "KAFKA_BROKERS_URL";
    public static final String SECURITYTOKENSERVICE_URL = "SECURITYTOKENSERVICE_URL";

    @Autowired
    UnleashService unleashService;

    private OidcAuthenticatorConfig createOpenAmAuthenticatorConfig() {
        String discoveryUrl = getRequiredProperty("OPENAM_DISCOVERY_URL");
        String clientId = getRequiredProperty("VEILARBLOGIN_OPENAM_CLIENT_ID");
        String refreshTokenUrl = getRequiredProperty("VEILARBLOGIN_OPENAM_REFRESH_URL");

        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(discoveryUrl)
                .withClientId(clientId)
                .withRefreshUrl(refreshTokenUrl)
                .withRefreshTokenCookieName(REFRESH_TOKEN_COOKIE_NAME)
                .withIdTokenCookieName(ID_TOKEN_COOKIE_NAME)
                .withIdentType(IdentType.InternBruker);
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator
                .addOidcAuthenticator(createOpenAmAuthenticatorConfig())
                .sts();
    }

    @Transactional
    @Override
    public void startup(ServletContext servletContext) {
        String dbUrl = getRequiredProperty(VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY);
        DbUtils.migrateAndClose(DbUtils.createDataSource(dbUrl, DbRole.ADMIN), DbRole.ADMIN);
        ServletUtil.filterBuilder(new ToggleFilter(unleashService)).register(servletContext);
    }

}

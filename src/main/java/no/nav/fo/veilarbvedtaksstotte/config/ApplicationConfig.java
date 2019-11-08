package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.ServletUtil;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.fo.veilarbvedtaksstotte.utils.DbUtils;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.servlet.ServletContext;


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

    @Inject
    protected JdbcTemplate jdbcTemplate;

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator
                .issoLogin()
                .sts();

    }

    @Autowired
    UnleashService unleashService;

    @Transactional
    @Override
    public void startup(ServletContext servletContext) {
        if(doDatabaseMigration()) {
            DbUtils.migrate(jdbcTemplate);
        }
        ServletUtil.filterBuilder(new ToggleFilter(unleashService)).register(servletContext);
    }

    public boolean doDatabaseMigration() {
        return true;
    }

}

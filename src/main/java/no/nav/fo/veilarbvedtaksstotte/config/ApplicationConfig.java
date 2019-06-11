package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.fo.veilarbvedtaksstotte.utils.DbUtils;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.servlet.ServletContext;

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
        RepositoryConfig.class,
        ScheduleConfig.class
})
public class ApplicationConfig implements ApiApplication {

    public static final String APPLICATION_NAME = "veilarbvedtaksstotte";

    @Inject
    protected JdbcTemplate jdbcTemplate;

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator
                .issoLogin()
                .sts();
    }

    @Transactional
    @Override
    public void startup(ServletContext servletContext) {
        DbUtils.migrate(jdbcTemplate);
    }

}

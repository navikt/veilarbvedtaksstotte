package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.fo.veilarbvedtaksstotte.utils.DbUtils;
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
        KafkaConfig.class,
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

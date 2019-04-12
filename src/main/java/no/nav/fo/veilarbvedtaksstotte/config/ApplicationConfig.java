package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.fo.veilarbvedtaksstotte.utils.MigrationUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.servlet.ServletContext;

@Configuration
@Import({
        ResourceConfig.class,
        DatabaseConfig.class,
        PepConfig.class,
        AktorConfig.class,
        ServiceConfig.class,
        ClientConfig.class,
        RepositoryConfig.class
})
public class ApplicationConfig implements ApiApplication {

    public static final String APPLICATION_NAME = "veilarbvedtaksstotte";

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator
                .issoLogin()
                .sts();
    }

    @Transactional
    @Override
    public void startup(ServletContext servletContext) {
        MigrationUtils.migrate(jdbcTemplate);
    }

}

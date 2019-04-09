package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.fo.veilarbvedtaksstotte.db.MigrationUtils;
import no.nav.fo.veilarbvedtaksstotte.db.VedtaksstotteRepository;
import no.nav.fo.veilarbvedtaksstotte.resources.HelloWorldResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.servlet.ServletContext;

@Configuration
@Import({
        HelloWorldResource.class,
        DatabaseConfig.class,
        VedtaksstotteRepository.class
})
public class ApplicationConfig implements ApiApplication {

    public static final String APPLICATION_NAME = "veilarbvedtaksstotte";

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Override
    public String getContextPath() {
        return "/" + APPLICATION_NAME;
    }

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

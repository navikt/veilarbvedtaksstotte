package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.apiapp.ServletUtil;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbvedtaksstotte.mock.AktorServiceMock;
import no.nav.fo.veilarbvedtaksstotte.mock.Mock;
import no.nav.fo.veilarbvedtaksstotte.mock.PepClientMock;
import no.nav.fo.veilarbvedtaksstotte.utils.DbRole;
import no.nav.fo.veilarbvedtaksstotte.utils.DbUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletContext;

import static no.nav.fo.veilarbvedtaksstotte.config.DatabaseConfig.VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class ApplicationTestConfig extends ApplicationConfig {

    public static final boolean RUN_WITH_MOCKS = false;

    @Transactional
    @Override
    public void startup(ServletContext servletContext) {
        String dbUrl = getRequiredProperty(VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY);
        DbUtils.migrateAndClose(DbUtils.createAdminDataSource(dbUrl), DbRole.ADMIN);
        ServletUtil.filterBuilder(new ToggleFilter(unleashService)).register(servletContext);
    }

    @Bean
    @Conditional(Mock.class)
    public AktorService aktorService() {
        return new AktorServiceMock();
    }

    @Bean
    @Conditional(Mock.class)
    public PepClient pepClient() {
        return new PepClientMock();
    }

}

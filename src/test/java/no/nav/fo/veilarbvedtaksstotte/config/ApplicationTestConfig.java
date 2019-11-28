package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.apiapp.ServletUtil;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbvedtaksstotte.mock.AktorServiceMock;
import no.nav.fo.veilarbvedtaksstotte.mock.Mock;
import no.nav.fo.veilarbvedtaksstotte.mock.PepClientMock;
import no.nav.fo.veilarbvedtaksstotte.utils.DbUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletContext;

@Configuration
public class ApplicationTestConfig extends ApplicationConfig {

    public static final boolean RUN_WITH_MOCKS = false;

    @Transactional
    @Override
    public void startup(ServletContext servletContext) {
        DbUtils.migrateAndClose(DbUtils.createAdminDataSource());
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

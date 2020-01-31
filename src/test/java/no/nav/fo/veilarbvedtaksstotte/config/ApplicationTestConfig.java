package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletContext;

@Configuration
@Import({ TestConfigs.class })
public class ApplicationTestConfig implements ApiApplication {

    @Transactional
    @Override
    public void startup(ServletContext servletContext) {
        System.out.println("STARTUP");
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {}

}

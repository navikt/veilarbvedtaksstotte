package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.apiapp.config.ApiAppConfigurator;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationTestConfig extends ApplicationConfig {

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator.sts();
    }

}

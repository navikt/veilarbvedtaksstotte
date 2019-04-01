package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.fo.veilarbvedtaksstotte.resources.HelloWorldResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        HelloWorldResource.class
})
public class ApplicationConfig implements ApiApplication {

    @Override
    public String getContextPath() {
        return "/veilarbvedtaksstotte";
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {}

}

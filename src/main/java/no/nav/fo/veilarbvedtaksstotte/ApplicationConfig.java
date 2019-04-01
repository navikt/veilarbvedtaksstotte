package no.nav.fo.veilarbvedtaksstotte;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        DemoRessurs.class,
        SelfdestructRessurs.class,
        DebugRessurs.class,
        ControlledSelfTestResource.class
})
public class ApplicationConfig implements ApiApplication {

    @Override
    public String getContextPath() {
        return "/veilarbvedtaksstotte";
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
    }

}

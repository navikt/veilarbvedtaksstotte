package no.nav.veilarbvedtaksstotte.config;

import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.resolveFromEnvironment;

@Configuration
public class FeatureToggleConfig {
    @Bean
    public UnleashService unleashService() {
        return new UnleashService(resolveFromEnvironment());
    }
}
